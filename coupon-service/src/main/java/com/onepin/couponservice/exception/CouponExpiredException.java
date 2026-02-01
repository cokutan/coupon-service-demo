package com.onepin.couponservice.exception;

public class CouponExpiredException extends CouponException {
  private static final String ERROR_CODE = "COUPON_EXPIRED";

  public CouponExpiredException(String message) {
    super(message, ERROR_CODE);
  }
}
