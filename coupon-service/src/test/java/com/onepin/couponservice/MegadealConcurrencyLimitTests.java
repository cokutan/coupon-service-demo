package com.onepin.couponservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.onepin.couponservice.dto.CouponRequestDto;
import com.onepin.couponservice.exception.ErrorResponse;
import com.onepin.couponservice.model.CouponType;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RSemaphore;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;

@TestPropertySource(
    properties = {"megadeal.wait-timeout-seconds=0", "megadeal.concurrent-limit=1"})
public class MegadealConcurrencyLimitTests extends BaseIntegrationTest {

  @Autowired private RedissonClient redissonClient;

  @Value("${megadeal.concurrent-limit}")
  private int megadealConcurrentLimit;

  @Value("${megadeal.rate-limit:10}")
  private int megadealRateLimit;

  @Value("${megadeal.rate-limit-seconds:1}")
  private int megadealRateLimitSeconds;

  @BeforeEach
  void resetRateLimiters() {
    RRateLimiter rateLimiter = redissonClient.getRateLimiter("megadeal:rate:limiter");
    rateLimiter.delete();
    rateLimiter.trySetRate(
        RateType.OVERALL, megadealRateLimit, megadealRateLimitSeconds, RateIntervalUnit.SECONDS);

    RSemaphore semaphore = redissonClient.getSemaphore("megadeal:semaphore");
    semaphore.delete();
    semaphore.trySetPermits(megadealConcurrentLimit);
  }

  @Test
  void testMegadealConcurrencyLimiting_Failure() throws Exception {
    // Given
    int concurrencyLimit = 1;
    int totalThreads = concurrencyLimit + 1;

    CountDownLatch readyLatch = new CountDownLatch(totalThreads);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(totalThreads);

    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failureCount = new AtomicInteger(0);

    try (ExecutorService executor = Executors.newFixedThreadPool(totalThreads)) {
      for (int i = 0; i < totalThreads; i++) {
        int userId = i;
        executor.submit(
            () -> {
              try {
                readyLatch.countDown(); // Thread is ready
                startLatch.await(); // Wait for simultaneous start

                CouponRequestDto request = new CouponRequestDto();
                request.setUserId("user-" + userId);
                request.setType(CouponType.MEGADEAL);

                MvcResult result =
                    mockMvc
                        .perform(
                            post("/api/coupons/request")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(request)))
                        .andReturn();

                if (result.getResponse().getStatus() == 200) {
                  successCount.incrementAndGet();
                } else {
                  failureCount.incrementAndGet();
                  ErrorResponse error =
                      jsonMapper.readValue(
                          result.getResponse().getContentAsString(), ErrorResponse.class);
                  assertEquals("CONCURRENT_REQUEST_LIMIT_EXCEEDED", error.getCode());
                }

              } catch (Exception e) {
                throw new RuntimeException(e);
              } finally {
                doneLatch.countDown(); // Signal completion
              }
            });
      }

      // Wait until all threads are ready
      readyLatch.await();

      // Start all threads at the same time
      startLatch.countDown();

      // Wait for all threads to finish
      doneLatch.await();
    }

    // Then
    assertEquals(concurrencyLimit, successCount.get());
    assertEquals(1, failureCount.get());
  }
}
