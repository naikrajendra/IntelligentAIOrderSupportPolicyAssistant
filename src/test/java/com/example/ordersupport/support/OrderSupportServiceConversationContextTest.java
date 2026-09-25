package com.example.ordersupport.support;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrderSupportServiceConversationContextTest {

    @Test
    void includesRecentMessagesAndSummarizesEarlierHistory() {
        List<ChatMessage> history = List.of(
                new ChatMessage("user", "I want to track my order.", Instant.now().minusSeconds(3600)),
                new ChatMessage("assistant", "Let me check the status.", Instant.now().minusSeconds(3500)),
                new ChatMessage("user", "It was delayed.", Instant.now().minusSeconds(3400)),
                new ChatMessage("assistant", "I can help with that.", Instant.now().minusSeconds(3300)),
                new ChatMessage("user", "Can I cancel it?", Instant.now().minusSeconds(3200)),
                new ChatMessage("assistant", "Only with confirmation.", Instant.now().minusSeconds(3100)),
                new ChatMessage("user", "I want a refund.", Instant.now().minusSeconds(3000)),
                new ChatMessage("assistant", "Refund depends on policy.", Instant.now().minusSeconds(2900)),
                new ChatMessage("user", "My order is still delayed.", Instant.now().minusSeconds(2800)),
                new ChatMessage("assistant", "Here is the updated status.", Instant.now().minusSeconds(2700)),
                new ChatMessage("user", "I need the latest update.", Instant.now().minusSeconds(2600))
        );

        String optimized = OrderSupportService.buildOptimizedConversationContext(history);

        assertTrue(optimized.contains("Earlier conversation summary"));
        assertTrue(optimized.contains("Recent conversation"));
        assertTrue(optimized.contains("latest update"));
    }
}
