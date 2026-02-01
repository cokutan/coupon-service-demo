package com.onepin.couponservice.controller;

import com.onepin.couponservice.dto.CouponRequestDto;
import com.onepin.couponservice.dto.CouponUploadDto;
import com.onepin.couponservice.dto.RedemptionRequestDto;
import com.onepin.couponservice.dto.StatsDto;
import com.onepin.couponservice.model.Coupon;
import com.onepin.couponservice.service.CouponService;
import jakarta.validation.Valid;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coupons")
public class CouponController {

  private static final Logger logger = LoggerFactory.getLogger(CouponController.class);
  private final CouponService couponService;

  @Autowired
  public CouponController(CouponService couponService) {
    this.couponService = couponService;
  }

  @PostMapping("/upload")
  public ResponseEntity<String> uploadCoupons(@RequestBody List<CouponUploadDto> couponDtos) {
    logger.info("Received request to upload {} coupons", couponDtos.size());
    couponService.uploadCoupons(couponDtos);
    return ResponseEntity.ok("Coupons uploaded successfully");
  }

  @PostMapping("/request")
  public ResponseEntity<Coupon> requestNewCoupon(@RequestBody @Valid CouponRequestDto request) {
    logger.info("Received request for new coupon from user: {}", request.getUserId());
    Coupon coupon = couponService.requestNewCoupon(request);
    return ResponseEntity.ok(coupon);
  }

  @PostMapping("/redeem")
  public ResponseEntity<String> redeemCoupon(@RequestBody @Valid RedemptionRequestDto request) {
    logger.info("Received redemption request for coupon: {}", request.getCouponCode());
    couponService.redeemCoupon(request);
    return ResponseEntity.ok("Coupon redeemed successfully");
  }

  @GetMapping("/stats")
  public ResponseEntity<StatsDto> getStats() {
    logger.info("Received request for coupon stats");
    return ResponseEntity.ok(couponService.getStats());
  }
}
