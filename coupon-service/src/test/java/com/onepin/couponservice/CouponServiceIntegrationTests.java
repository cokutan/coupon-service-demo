package com.onepin.couponservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.onepin.couponservice.dto.CouponRequestDto;
import com.onepin.couponservice.dto.CouponUploadDto;
import com.onepin.couponservice.dto.RedemptionRequestDto;
import com.onepin.couponservice.exception.ErrorResponse;
import com.onepin.couponservice.model.Coupon;
import com.onepin.couponservice.model.CouponType;
import com.onepin.couponservice.repository.CouponRepository;
import com.onepin.couponservice.service.CouponService;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

public class CouponServiceIntegrationTests extends BaseIntegrationTest {

  @Autowired private CouponService couponService;

  @Autowired private CouponRepository couponRepository;

  @Value("${coupon.upload.max-batch-size}")
  private int couponUploadMaxBatchSize;

  @BeforeEach
  void setUp() {
    couponRepository.deleteAll();
  }

  @Test
  void testBulkUpload() throws Exception {
    // Given
    int count = 2000;
    List<CouponUploadDto> dtos =
        IntStream.range(0, count)
            .mapToObj(
                i -> {
                  CouponUploadDto dto = new CouponUploadDto();
                  dto.setCode("BULK-" + i);
                  dto.setType(CouponType.STANDARD);
                  dto.setMaxUsages(1);
                  return dto;
                })
            .toList();

    // When
    mockMvc
        .perform(
            post("/api/coupons/upload")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(dtos)))
        .andExpect(status().isOk());

    // Then
    assertEquals(count, couponRepository.count());
  }

  @Test
  void testBulkUpload_ExceedsLimit() throws Exception {
    // Given
    int count = couponUploadMaxBatchSize + 1;
    List<CouponUploadDto> dtos =
        IntStream.range(0, count)
            .mapToObj(
                i -> {
                  CouponUploadDto dto = new CouponUploadDto();
                  dto.setCode("BULK-" + i);
                  return dto;
                })
            .toList();

    // When & Then
    mockMvc
        .perform(
            post("/api/coupons/upload")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(dtos)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void testStandardCouponRedemption_HappyPath() throws Exception {
    // Given
    Coupon coupon =
        Coupon.builder()
            .code("STANDARD-123")
            .type(CouponType.STANDARD)
            .maxUsages(1)
            .currentUsages(0)
            .expiryDate(LocalDateTime.now().plusDays(1))
            .build();
    couponRepository.save(coupon);

    RedemptionRequestDto request = new RedemptionRequestDto();
    request.setCouponCode("STANDARD-123");
    request.setUserId("user-1");

    // When
    mockMvc
        .perform(
            post("/api/coupons/redeem")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isOk());

    // Then
    Coupon redeemedCoupon = couponRepository.findById("STANDARD-123").orElseThrow();
    assertEquals(1, redeemedCoupon.getCurrentUsages());
  }

  @Test
  void testMegadealRateLimiting_CausesWait() throws InterruptedException, ExecutionException {
    // Given
    int rateLimit = 10;
    int totalRequests = rateLimit + 1;
    List<Callable<Coupon>> tasks = new ArrayList<>();

    for (int i = 0; i < totalRequests; i++) {
      int userId = i;
      tasks.add(
          () -> {
            CouponRequestDto request = new CouponRequestDto();
            request.setUserId("user-" + userId);
            request.setType(CouponType.MEGADEAL);
            return couponService.requestNewCoupon(request);
          });
    }

    // When
    Instant start = Instant.now();
    List<Future<Coupon>> futures;
    try (ExecutorService executor = Executors.newFixedThreadPool(totalRequests)) {
      futures = executor.invokeAll(tasks);
    }

    // Then
    AtomicInteger successCount = new AtomicInteger(0);
    for (Future<Coupon> future : futures) {
      Coupon coupon = future.get();
      assertNotNull(coupon);
      successCount.incrementAndGet();
    }
    Instant finish = Instant.now();
    long timeElapsed = Duration.between(start, finish).toMillis();

    assertEquals(totalRequests, successCount.get());
    assertThat(timeElapsed).isGreaterThan(1000L);
  }

  @Test
  void testRedeemExpiredCoupon() throws Exception {
    // Given
    Coupon coupon =
        Coupon.builder()
            .code("EXPIRED-123")
            .type(CouponType.STANDARD)
            .maxUsages(1)
            .currentUsages(0)
            .expiryDate(LocalDateTime.now().minusDays(1))
            .build();
    couponRepository.save(coupon);

    RedemptionRequestDto request = new RedemptionRequestDto();
    request.setCouponCode("EXPIRED-123");
    request.setUserId("user-1");

    // When & Then
    MvcResult result =
        mockMvc
            .perform(
                post("/api/coupons/redeem")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andReturn();

    ErrorResponse error =
        jsonMapper.readValue(result.getResponse().getContentAsString(), ErrorResponse.class);
    assertEquals("COUPON_EXPIRED", error.getCode());
  }

  @Test
  void testMegadealRequest_SucceedsAfterWaiting() throws InterruptedException, ExecutionException {
    // Given: Exhaust the rate limit for the current second
    int rateLimit = 10;
    try (ExecutorService setupExecutor = Executors.newFixedThreadPool(rateLimit)) {
      for (int i = 0; i < rateLimit; i++) {
        int userId = i;
        setupExecutor.submit(
            () -> {
              CouponRequestDto request = new CouponRequestDto();
              request.setUserId("user-setup-" + userId);
              request.setType(CouponType.MEGADEAL);
              return couponService.requestNewCoupon(request);
            });
      }
    }

    // When: Launch an 11th request that should wait
    Future<Coupon> future;
    try (ExecutorService testExecutor = Executors.newSingleThreadExecutor()) {
      future =
          testExecutor.submit(
              () -> {
                CouponRequestDto request = new CouponRequestDto();
                request.setUserId("user-wait");
                request.setType(CouponType.MEGADEAL);
                // This call will block and wait
                return couponService.requestNewCoupon(request);
              });
    }

    // Then: The request should succeed after the rate limit window resets
    Coupon coupon = future.get(); // This will block until the future is complete
    assertNotNull(coupon);
    assertEquals(CouponType.MEGADEAL, coupon.getType());
  }
}
