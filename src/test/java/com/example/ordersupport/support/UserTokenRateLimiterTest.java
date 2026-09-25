package com.example.ordersupport.support;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class UserTokenRateLimiterTest {

    @Test
    void allowsUsageWithinQuota() {
        UserTokenRateLimiter limiter = new UserTokenRateLimiter(50_000L, Duration.ofHours(1));

        assertTrue(limiter.tryConsume("user-1", 20_000L));
        assertTrue(limiter.tryConsume("user-1", 30_000L));
    }

    @Test
    void rejectsWhenQuotaExceeded() {
        UserTokenRateLimiter limiter = new UserTokenRateLimiter(50_000L, Duration.ofHours(1));

        assertTrue(limiter.tryConsume("user-2", 45_000L));
        assertFalse(limiter.tryConsume("user-2", 6_000L));
    }
}
