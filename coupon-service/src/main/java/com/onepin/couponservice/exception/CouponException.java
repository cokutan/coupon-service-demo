package com.onepin.couponservice.exception;

import lombok.Getter;

@Getter
public abstract class CouponException extends RuntimeException {
  private final String code;

  public CouponException(String message, String code) {
    super(message);
    this.code = code;
  }
}
