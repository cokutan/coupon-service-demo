package com.onepin.couponservice;

import static org.junit.jupiter.api.Assertions.*;

import com.onepin.couponservice.dto.CouponRequestDto;
import com.onepin.couponservice.dto.RedemptionRequestDto;
import com.onepin.couponservice.model.Coupon;
import com.onepin.couponservice.model.CouponType;
import com.onepin.couponservice.repository.CouponRepository;
import com.onepin.couponservice.service.CouponService;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CouponServiceApplicationTests {

  @Autowired private CouponService couponService;

  @Autowired private CouponRepository couponRepository;

  @MockBean private RedissonClient redissonClient;

  @Test
  void contextLoads() {}

  @Test
  void testRequestAndRedeemCoupon() {
    // 1. Request a new coupon
    CouponRequestDto requestDto = new CouponRequestDto();
    requestDto.setUserId("user123");
    requestDto.setType(CouponType.STANDARD);

    Coupon coupon = couponService.requestNewCoupon(requestDto);
    assertNotNull(coupon);
    assertNotNull(coupon.getCode());
    assertEquals(CouponType.STANDARD, coupon.getType());

    // 2. Redeem the coupon
    RedemptionRequestDto redemptionDto = new RedemptionRequestDto();
    redemptionDto.setCouponCode(coupon.getCode());
    redemptionDto.setUserId("user123");

    couponService.redeemCoupon(redemptionDto);

    // 3. Verify usage count
    Coupon updatedCoupon = couponRepository.findById(coupon.getCode()).orElseThrow();
    assertEquals(1, updatedCoupon.getCurrentUsages());
  }
}
