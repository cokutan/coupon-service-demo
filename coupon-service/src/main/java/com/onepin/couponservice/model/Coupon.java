package com.onepin.couponservice.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "coupons")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {

  @Id private String code;

  @Enumerated(EnumType.STRING)
  private CouponType type;

  private Double discountAmount;
  private boolean isPercentage; // true if percentage, false if flat amount

  private LocalDateTime expiryDate;
  private int maxUsages;
  private int currentUsages;

  @Version private Long version;
}
