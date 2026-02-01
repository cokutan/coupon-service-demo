package com.onepin.couponservice.exception;

public class ConcurrentRequestLimitException extends CouponException {
  private static final String ERROR_CODE = "CONCURRENT_REQUEST_LIMIT_EXCEEDED";

  public ConcurrentRequestLimitException(String message) {
    super(message, ERROR_CODE);
  }
}
