package com.onepin.couponservice.exception;

public class BatchSizeExceededException extends CouponException {
  private static final String ERROR_CODE = "BATCH_SIZE_EXCEEDED";

  public BatchSizeExceededException(String message) {
    super(message, ERROR_CODE);
  }
}
