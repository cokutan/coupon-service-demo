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
    properties = {"megadeal.wait-timeout-seconds=0", "megadeal.concurrent-limit=5"})
public class MegadealConcurrencyLimitTests extends BaseIntegrationTest {

  @Test
  void testMegadealConcurrencyLimiting_Failure() throws Exception {
    // Given
    int concurrencyLimit = 5;
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
