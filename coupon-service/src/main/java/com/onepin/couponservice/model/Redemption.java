package com.onepin.couponservice.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "redemptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Redemption {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String couponCode;

  @Enumerated(EnumType.STRING)
  private CouponType couponType;

  private String userId;
  private LocalDateTime redeemedAt;
}
