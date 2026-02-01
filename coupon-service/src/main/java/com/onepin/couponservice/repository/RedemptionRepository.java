package com.onepin.couponservice.repository;

import com.onepin.couponservice.model.CouponType;
import com.onepin.couponservice.model.Redemption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RedemptionRepository extends JpaRepository<Redemption, Long> {
  long countByCouponType(CouponType type);
}
