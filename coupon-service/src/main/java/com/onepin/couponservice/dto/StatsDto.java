package com.onepin.couponservice.dto;

import com.onepin.couponservice.model.CouponType;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatsDto {
  private Map<CouponType, Long> createdCounts;
  private Map<CouponType, Long> redeemedCounts;
}
