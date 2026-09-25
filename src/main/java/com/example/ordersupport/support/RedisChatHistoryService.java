package com.example.ordersupport.support;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisChatHistoryService {

    private static final Duration TTL = Duration.ofHours(1);
    private static final String KEY_PREFIX = "chat-history:";

    private final StringRedisTemplate redisTemplate;

    public RedisChatHistoryService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public List<ChatMessage> getMessages(String userId) {
        String value = redisTemplate.opsForValue().get(key(userId));
        if (value == null || value.isBlank()) {
            return new ArrayList<>();
        }
        return parseMessages(value);
    }

    public void appendMessage(String userId, ChatMessage message) {
        List<ChatMessage> messages = getMessages(userId);
        messages.add(message);
        persist(userId, messages);
    }

    public void replaceMessages(String userId, List<ChatMessage> messages) {
        persist(userId, messages == null ? List.of() : messages);
    }

    public void clear(String userId) {
        redisTemplate.delete(key(userId));
    }

    public boolean hasConversation(String userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key(userId)));
    }

    private void persist(String userId, List<ChatMessage> messages) {
        String serialized = serialize(messages);
        redisTemplate.opsForValue().set(key(userId), serialized, TTL);
    }

    private String key(String userId) {
        return KEY_PREFIX + userId;
    }

    private String serialize(List<ChatMessage> messages) {
        StringBuilder builder = new StringBuilder();
        for (ChatMessage message : messages) {
            if (builder.length() > 0) {
                builder.append("||");
            }
            builder.append(message.role())
                    .append("::")
                    .append(escape(message.content()))
                    .append("::")
                    .append(message.createdAt() == null ? Instant.now() : message.createdAt());
        }
        return builder.toString();
    }

    private List<ChatMessage> parseMessages(String serialized) {
        if (serialized == null || serialized.isBlank()) {
            return new ArrayList<>();
        }

        List<ChatMessage> result = new ArrayList<>();
        for (String chunk : serialized.split("\\|\\|")) {
            if (chunk.isBlank()) {
                continue;
            }
            String[] parts = chunk.split("::", 3);
            if (parts.length < 3) {
                continue;
            }
            result.add(new ChatMessage(parts[0], unescape(parts[1]), Instant.parse(parts[2])));
        }
        return result;
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("||", "\\||").replace("::", "\\::");
    }

    private String unescape(String value) {
        return value == null ? "" : value.replace("\\||", "||").replace("\\::", "::");
    }
}
