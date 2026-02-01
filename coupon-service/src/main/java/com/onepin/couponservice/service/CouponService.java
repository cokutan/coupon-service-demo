package com.onepin.couponservice.service;

import com.onepin.couponservice.dto.CouponRequestDto;
import com.onepin.couponservice.dto.CouponUploadDto;
import com.onepin.couponservice.dto.RedemptionRequestDto;
import com.onepin.couponservice.dto.StatsDto;
import com.onepin.couponservice.exception.BatchSizeExceededException;
import com.onepin.couponservice.exception.ConcurrentRequestLimitException;
import com.onepin.couponservice.exception.CouponExpiredException;
import com.onepin.couponservice.exception.CouponNotFoundException;
import com.onepin.couponservice.exception.CouponUsageLimitException;
import com.onepin.couponservice.exception.InterruptedCouponRequestException;
import com.onepin.couponservice.exception.RateLimitExceededException;
import com.onepin.couponservice.model.Coupon;
import com.onepin.couponservice.model.CouponType;
import com.onepin.couponservice.model.Redemption;
import com.onepin.couponservice.repository.CouponRepository;
import com.onepin.couponservice.repository.RedemptionRepository;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RSemaphore;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CouponService {

  private static final Logger logger = LoggerFactory.getLogger(CouponService.class);

  private final CouponRepository couponRepository;
  private final RedemptionRepository redemptionRepository;
  private final RedissonClient redissonClient;

  private static final String MEGADEAL_RATE_LIMIT_KEY = "megadeal:rate:limiter";
  private static final String MEGADEAL_CONCURRENT_KEY = "megadeal:semaphore";

  @Value("${megadeal.concurrent-limit}")
  private int megadealConcurrentLimit;

  @Value("${megadeal.rate-limit}")
  private int megadealRateLimit;

  @Value("${megadeal.rate-limit-seconds}")
  private int megadealRateLimitSeconds;

  @Value("${megadeal.wait-timeout-seconds}")
  private int megadealWaitTimeoutSeconds;

  @Autowired
  public CouponService(
      CouponRepository couponRepository,
      RedemptionRepository redemptionRepository,
      RedissonClient redissonClient) {
    this.couponRepository = couponRepository;
    this.redemptionRepository = redemptionRepository;
    this.redissonClient = redissonClient;
  }

  @Transactional
  public void uploadCoupons(List<CouponUploadDto> couponDtos) {
    logger.info("Uploading {} coupons", couponDtos.size());
    if (couponDtos.size() > 50000) {
      logger.error("Batch size {} exceeds limit of 50000", couponDtos.size());
      throw new BatchSizeExceededException("Batch size exceeds limit of 50000");
    }
    List<Coupon> coupons =
        couponDtos.stream()
            .map(
                dto ->
                    Coupon.builder()
                        .code(dto.getCode())
                        .type(dto.getType())
                        .discountAmount(dto.getDiscountAmount())
                        .isPercentage(dto.isPercentage())
                        .expiryDate(dto.getExpiryDate())
                        .maxUsages(dto.getMaxUsages())
                        .currentUsages(0)
                        .build())
            .collect(Collectors.toList());
    couponRepository.saveAll(coupons);
    logger.info("Successfully uploaded {} coupons", coupons.size());
  }

  @Transactional
  public Coupon requestNewCoupon(CouponRequestDto request) {
    logger.info(
        "Requesting new coupon for user: {}, type: {}", request.getUserId(), request.getType());
    if (request.getType() == CouponType.MEGADEAL) {
      return handleMegadealRequest(request);
    } else {
      return generateCoupon(request.getType());
    }
  }

  private Coupon handleMegadealRequest(CouponRequestDto request) {
    logger.debug("Handling MEGADEAL request for user: {}", request.getUserId());

    RSemaphore semaphore = redissonClient.getSemaphore(MEGADEAL_CONCURRENT_KEY);
    semaphore.trySetPermits(megadealConcurrentLimit);

    RRateLimiter rateLimiter = redissonClient.getRateLimiter(MEGADEAL_RATE_LIMIT_KEY);
    rateLimiter.trySetRate(
        RateType.OVERALL, megadealRateLimit, megadealRateLimitSeconds, RateIntervalUnit.SECONDS);

    try {
      if (semaphore.tryAcquire(megadealWaitTimeoutSeconds, TimeUnit.SECONDS)) {
        try {
          if (rateLimiter.tryAcquire(1, megadealWaitTimeoutSeconds, TimeUnit.SECONDS)) {
            return generateCoupon(CouponType.MEGADEAL);
          } else {
            logger.warn("Rate limit exceeded for MEGADEAL coupon");
            throw new RateLimitExceededException("Server busy, please try again later.");
          }
        } finally {
          semaphore.release();
        }
      } else {
        logger.warn("Could not acquire semaphore for MEGADEAL coupon");
        throw new ConcurrentRequestLimitException(
            "Too many concurrent requests for MEGADEAL. Please try again.");
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      logger.error("Interrupted while waiting for coupon", e);
      throw new InterruptedCouponRequestException("Interrupted while waiting for coupon");
    }
  }

  private Coupon generateCoupon(CouponType type) {
    String code = UUID.randomUUID().toString();
    Coupon coupon =
        Coupon.builder()
            .code(code)
            .type(type != null ? type : CouponType.STANDARD)
            .discountAmount(10.0) // Default discount
            .isPercentage(false)
            .expiryDate(LocalDateTime.now().plusDays(30))
            .maxUsages(1)
            .currentUsages(0)
            .build();
    Coupon savedCoupon = couponRepository.save(coupon);
    logger.info(
        "Generated new coupon: {} of type: {}", savedCoupon.getCode(), savedCoupon.getType());
    return savedCoupon;
  }

  @Transactional
  public void redeemCoupon(RedemptionRequestDto request) {
    logger.info("Redeeming coupon: {} for user: {}", request.getCouponCode(), request.getUserId());
    Coupon coupon =
        couponRepository
            .findById(request.getCouponCode())
            .orElseThrow(() -> new CouponNotFoundException("Coupon not found"));

    if (coupon.getExpiryDate() != null && coupon.getExpiryDate().isBefore(LocalDateTime.now())) {
      logger.warn("Coupon expired: {}", request.getCouponCode());
      throw new CouponExpiredException("Coupon expired");
    }

    if (coupon.getCurrentUsages() >= coupon.getMaxUsages()) {
      logger.warn("Coupon usage limit reached: {}", request.getCouponCode());
      throw new CouponUsageLimitException("Coupon usage limit reached");
    }

    coupon.setCurrentUsages(coupon.getCurrentUsages() + 1);
    couponRepository.save(coupon);

    Redemption redemption =
        Redemption.builder()
            .couponCode(coupon.getCode())
            .couponType(coupon.getType())
            .userId(request.getUserId())
            .redeemedAt(LocalDateTime.now())
            .build();
    redemptionRepository.save(redemption);
    logger.info("Successfully redeemed coupon: {}", request.getCouponCode());
  }

  public StatsDto getStats() {
    logger.info("Fetching coupon statistics");
    Map<CouponType, Long> created = new HashMap<>();
    Map<CouponType, Long> redeemed = new HashMap<>();

    for (CouponType type : CouponType.values()) {
      created.put(type, couponRepository.countByType(type));
      redeemed.put(type, redemptionRepository.countByCouponType(type));
    }

    return new StatsDto(created, redeemed);
  }

  @Scheduled(fixedRate = 60000) // Run every minute
  public void purgeExpiredCoupons() {
    logger.info("Starting scheduled purge of expired coupons");
    List<Coupon> expiredCoupons = couponRepository.findByExpiryDateBefore(LocalDateTime.now());
    int purgedCount = 0;
    for (Coupon coupon : expiredCoupons) {
      if (coupon.getCurrentUsages() == 0) {
        couponRepository.delete(coupon);
        purgedCount++;
      }
    }
    if (purgedCount > 0) {
      logger.info("Purged {} expired unused coupons", purgedCount);
    } else {
      logger.info("No expired unused coupons found to purge");
    }
  }
}
