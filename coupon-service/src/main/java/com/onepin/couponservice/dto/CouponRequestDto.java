package com.onepin.couponservice.dto;

import com.onepin.couponservice.model.CouponType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CouponRequestDto {
  private CouponType type;
  @NotNull private String userId;
}
