package com.onepin.couponservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RedemptionRequestDto {
  @NotNull private String couponCode;
  @NotNull private String userId;
}
