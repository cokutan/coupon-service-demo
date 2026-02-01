package com.onepin.couponservice.exception;

public class InterruptedCouponRequestException extends CouponException {
  private static final String ERROR_CODE = "INTERRUPTED_EXCEPTION";

  public InterruptedCouponRequestException(String message) {
    super(message, ERROR_CODE);
  }
}
