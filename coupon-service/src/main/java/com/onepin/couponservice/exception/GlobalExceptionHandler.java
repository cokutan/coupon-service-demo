package com.onepin.couponservice.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(CouponNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleCouponNotFoundException(CouponNotFoundException ex) {
    return buildErrorResponse(ex.getCode(), ex.getMessage(), HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(CouponUsageLimitException.class)
  public ResponseEntity<ErrorResponse> handleCouponUsageLimitException(
      CouponUsageLimitException ex) {
    return buildErrorResponse("COUPON_USAGE_LIMIT_REACHED", ex.getMessage(), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(CouponExpiredException.class)
  public ResponseEntity<ErrorResponse> handleCouponExpiredException(CouponExpiredException ex) {
    return buildErrorResponse(ex.getCode(), ex.getMessage(), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationExceptions(
      MethodArgumentNotValidException ex) {
    String message =
        "Validation error: " + ex.getBindingResult().getFieldError().getDefaultMessage();
    return buildErrorResponse("VALIDATION_ERROR", message, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(BatchSizeExceededException.class)
  public ResponseEntity<ErrorResponse> handleBatchSizeExceededException(
      BatchSizeExceededException ex) {
    return buildErrorResponse(ex.getCode(), ex.getMessage(), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(RateLimitExceededException.class)
  public ResponseEntity<ErrorResponse> handleRateLimitExceededException(
      RateLimitExceededException ex) {
    return buildErrorResponse(ex.getCode(), ex.getMessage(), HttpStatus.TOO_MANY_REQUESTS);
  }

  @ExceptionHandler(ConcurrentRequestLimitException.class)
  public ResponseEntity<ErrorResponse> handleConcurrentRequestLimitException(
      ConcurrentRequestLimitException ex) {
    return buildErrorResponse(
        ex.getCode(), ex.getMessage(), HttpStatus.TOO_MANY_REQUESTS);
  }

  @ExceptionHandler(InterruptedCouponRequestException.class)
  public ResponseEntity<ErrorResponse> handleInterruptedCouponRequestException(
      InterruptedCouponRequestException ex) {
    return buildErrorResponse(
        "REQUEST_INTERRUPTED", ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleAllExceptions(Exception ex) {
    logger.error("An unexpected error occurred", ex);
    return buildErrorResponse(
        "INTERNAL_SERVER_ERROR", "An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
  }

  private ResponseEntity<ErrorResponse> buildErrorResponse(
          String code, String message, HttpStatus status) {
    ErrorResponse errorResponse = ErrorResponse.builder().code(code).message(message).build();
    return new ResponseEntity<>(errorResponse, status);
  }
}
