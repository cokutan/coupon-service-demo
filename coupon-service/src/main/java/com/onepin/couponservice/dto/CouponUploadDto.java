package com.onepin.couponservice.dto;

import com.onepin.couponservice.model.CouponType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class CouponUploadDto {
  @NotNull private String code;
  private CouponType type;
  private Double discountAmount;
  private boolean isPercentage;
  private LocalDateTime expiryDate;
  private int maxUsages;
}
