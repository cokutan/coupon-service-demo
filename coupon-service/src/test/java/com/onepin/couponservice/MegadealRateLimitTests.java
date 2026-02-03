package com.onepin.couponservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.onepin.couponservice.dto.CouponRequestDto;
import com.onepin.couponservice.exception.ErrorResponse;
import com.onepin.couponservice.model.CouponType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RSemaphore;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;

@TestPropertySource(properties = {
        "megadeal.wait-timeout-seconds=0",
        "megadeal.concurrent-limit=10",
        "megadeal.rate-limit=1",
})
class MegadealRateLimitTests extends BaseIntegrationTest {

    @Autowired
    private RedissonClient redissonClient;

    @BeforeEach
    void resetRedisState() {
        RRateLimiter rateLimiter = redissonClient.getRateLimiter("megadeal:rate:limiter");
        rateLimiter.delete();
        rateLimiter.trySetRate(
                RateType.OVERALL, 1, 60, RateIntervalUnit.SECONDS
        );

        RSemaphore semaphore = redissonClient.getSemaphore("megadeal:semaphore");
        semaphore.delete();
        semaphore.trySetPermits(10);
    }

    @Test
    void secondRequestIsRateLimited_deterministic() throws Exception {
        CouponRequestDto request = new CouponRequestDto();
        request.setUserId("user-1");
        request.setType(CouponType.MEGADEAL);

        // 1️⃣ First request → SUCCESS
        MvcResult first =
                mockMvc.perform(
                        post("/api/coupons/request")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(request))
                ).andReturn();

        assertEquals(200, first.getResponse().getStatus());

        // 2️⃣ Second request → RATE LIMITED
        MvcResult second =
                mockMvc.perform(
                        post("/api/coupons/request")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(request))
                ).andReturn();

        assertEquals(429, second.getResponse().getStatus());

        ErrorResponse error =
                jsonMapper.readValue(
                        second.getResponse().getContentAsString(),
                        ErrorResponse.class
                );

        assertEquals("RATE_LIMIT_EXCEEDED", error.getCode());
    }
}
