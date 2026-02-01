package com.onepin.couponservice.exception;

public class RateLimitExceededException extends CouponException {
  private static final String ERROR_CODE = "RATE_LIMIT_EXCEEDED";

  public RateLimitExceededException(String message) {
    super(message, ERROR_CODE);
  }
}
