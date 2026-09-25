package com.example.ordersupport.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OrderSupportMetricsTest {

    @Test
    void shouldBuildQueryableMetricsSnapshotForAnalytics() {
        SupportQueryEvent event = OrderSupportService.buildQueryEvent(
                "C-991",
                "O-1001",
                "What is the status of my order?",
                "Your order is in transit and will arrive by Friday.",
                "status-check",
                "order status and shipping policy available",
                true
        );

        assertThat(event).isNotNull();
        assertThat(event.getCustomerId()).isEqualTo("C-991");
        assertThat(event.getOrderId()).isEqualTo("O-1001");
        assertThat(event.getConfidenceScore()).isBetween(0.0, 1.0);
        assertThat(event.getTotalTokens()).isGreaterThan(0L);
        assertThat(event.getStatus()).isEqualTo("success");
    }
}
