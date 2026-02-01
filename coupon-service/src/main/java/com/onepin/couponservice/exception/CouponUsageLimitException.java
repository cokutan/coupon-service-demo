package com.onepin.couponservice.exception;

public class CouponUsageLimitException extends CouponException {
  private static final String ERROR_CODE = "COUPON_USAGE_LIMIT_REACHED";

  public CouponUsageLimitException(String message) {
    super(message, ERROR_CODE);
  }
}
