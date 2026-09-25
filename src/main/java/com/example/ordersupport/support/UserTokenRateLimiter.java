package com.example.ordersupport.support;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class UserTokenRateLimiter {

    private final long quotaTokens;
    private final Duration window;
    private final Map<String, WindowState> usageByUser = new ConcurrentHashMap<>();

    public UserTokenRateLimiter() {
        this(50_000L, Duration.ofHours(1));
    }

    public UserTokenRateLimiter(long quotaTokens, Duration window) {
        if (quotaTokens <= 0) {
            throw new IllegalArgumentException("quotaTokens must be > 0");
        }
        if (window == null || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("window must be positive");
        }
        this.quotaTokens = quotaTokens;
        this.window = window;
    }

    public synchronized boolean tryConsume(String userId, long tokens) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }
        if (tokens <= 0) {
            return false;
        }

        WindowState state = usageByUser.computeIfAbsent(userId, key -> new WindowState());
        Instant now = Instant.now();

        if (state.isExpired(now, window)) {
            state.reset(now);
        }

        if (state.tokensUsed + tokens > quotaTokens) {
            return false;
        }

        state.tokensUsed += tokens;
        state.windowStart = now;
        return true;
    }

    public synchronized long remainingTokens(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }

        WindowState state = usageByUser.get(userId);
        if (state == null) {
            return quotaTokens;
        }

        Instant now = Instant.now();
        if (state.isExpired(now, window)) {
            state.reset(now);
            return quotaTokens;
        }

        return Math.max(0, quotaTokens - state.tokensUsed);
    }

    private static final class WindowState {
        private Instant windowStart = Instant.now();
        private long tokensUsed = 0L;

        private boolean isExpired(Instant now, Duration window) {
            return Duration.between(windowStart, now).compareTo(window) >= 0;
        }

        private void reset(Instant now) {
            windowStart = now;
            tokensUsed = 0L;
        }
    }
}
