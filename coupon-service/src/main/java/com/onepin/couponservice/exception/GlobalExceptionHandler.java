package com.onepin.couponservice.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(CouponException.class)
  public ResponseEntity<ErrorResponse> handleCouponException(CouponException ex) {
    ErrorResponse errorResponse =
        ErrorResponse.builder()
            .status(HttpStatus.BAD_REQUEST.value())
            .message(ex.getMessage())
            .code(ex.getCode())
            .build();
    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(OptimisticLockingFailureException.class)
  public ResponseEntity<ErrorResponse> handleOptimisticLockingFailureException(
      OptimisticLockingFailureException ex) {
    logger.warn("Optimistic locking failure: {}", ex.getMessage());
    ErrorResponse errorResponse =
        ErrorResponse.builder()
            .status(HttpStatus.CONFLICT.value())
            .message("This coupon was updated by another transaction. Please try again.")
            .code("OPTIMISTIC_LOCKING_FAILURE")
            .build();
    return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleException(Exception ex) {
    logger.error("An unexpected error occurred: {}", ex.getMessage(), ex);
    ErrorResponse errorResponse =
        ErrorResponse.builder()
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .message("An unexpected error occurred. Please try again later.")
            .code("INTERNAL_SERVER_ERROR")
            .build();
    return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
