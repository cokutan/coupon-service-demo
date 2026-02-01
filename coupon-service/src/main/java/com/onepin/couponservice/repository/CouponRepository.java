package com.onepin.couponservice.repository;

import com.onepin.couponservice.model.Coupon;
import com.onepin.couponservice.model.CouponType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, String> {
  long countByType(CouponType type);

  List<Coupon> findByExpiryDateBefore(LocalDateTime dateTime);
}
