package com.onepin.couponservice.exception;

public class CouponNotFoundException extends CouponException {
  private static final String ERROR_CODE = "COUPON_NOT_FOUND";

  public CouponNotFoundException(String message) {
    super(message, ERROR_CODE);
  }
}
