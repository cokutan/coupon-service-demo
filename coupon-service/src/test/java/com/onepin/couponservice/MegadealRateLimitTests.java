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
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;

@TestPropertySource(
    properties = {
      "megadeal.wait-timeout-seconds=0",
      "megadeal.concurrent-limit=20",
      "megadeal.rate-limit=10"
    })
public class MegadealRateLimitTests extends BaseIntegrationTest {

  @Test
  void testMegadealRateLimiting_ThrowsException() throws Exception {
    // Given: Exhaust the rate limit
    int rateLimit = 10;
    int totalRequests = rateLimit + 1;

    CountDownLatch readyLatch = new CountDownLatch(totalRequests);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(totalRequests);

    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger rateLimitCount = new AtomicInteger(0);

    try (ExecutorService executor = Executors.newFixedThreadPool(totalRequests)) {
      for (int i = 0; i < totalRequests; i++) {
        int userId = i;
        executor.submit(
            () -> {
              try {
                readyLatch.countDown();
                startLatch.await();

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
                  ErrorResponse error =
                      jsonMapper.readValue(
                          result.getResponse().getContentAsString(), ErrorResponse.class);
                  assertEquals("RATE_LIMIT_EXCEEDED", error.getCode());
                  rateLimitCount.incrementAndGet();
                }

              } catch (Exception e) {
                throw new RuntimeException(e);
              } finally {
                doneLatch.countDown();
              }
            });
      }

      readyLatch.await(); // all threads ready
      startLatch.countDown(); // fire at once
      doneLatch.await(); // wait for completion
    }

    assertEquals(rateLimit, successCount.get());
    assertEquals(1, rateLimitCount.get());
  }
}
