package com.example.ordersupport.support;

import java.time.Duration;
import java.util.Collections;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
public class RedisTokenRateLimiter extends UserTokenRateLimiter {

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> luaScript;

    public RedisTokenRateLimiter(StringRedisTemplate redisTemplate) {
        super(50_000L, Duration.ofHours(1));
        this.redisTemplate = redisTemplate;
        this.luaScript = new DefaultRedisScript<>();
        this.luaScript.setScriptText(
                "local key = KEYS[1]\n" +
                        "local quota = tonumber(ARGV[1])\n" +
                        "local tokens = tonumber(ARGV[2])\n" +
                        "local ttlSeconds = tonumber(ARGV[3])\n" +
                        "local current = redis.call('GET', key)\n" +
                        "if current == false then\n" +
                        "  current = 0\n" +
                        "end\n" +
                        "local used = tonumber(current)\n" +
                        "if used + tokens > quota then\n" +
                        "  return 0\n" +
                        "end\n" +
                        "redis.call('SET', key, used + tokens, 'EX', ttlSeconds)\n" +
                        "return 1\n"
        );
        this.luaScript.setResultType(Long.class);
    }

    public boolean tryConsume(String userId, long tokens) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }
        if (tokens <= 0) {
            return false;
        }

        String key = "token-quota:" + userId;
        Long allowed = redisTemplate.execute(
                luaScript,
                Collections.singletonList(key),
                String.valueOf(50_000L),
                String.valueOf(tokens),
                String.valueOf(Duration.ofHours(1).getSeconds())
        );
        return Boolean.TRUE.equals(allowed) && allowed != null && allowed > 0;
    }
}
