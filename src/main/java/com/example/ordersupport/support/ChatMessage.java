package com.example.ordersupport.support;

import java.time.Instant;

public record ChatMessage(
        String role,
        String content,
        Instant createdAt
) {
}
