package com.example.ordersupport.support;

public record OrderSupportRequest(
        String customerId,
        String orderId,
        String question,
        Boolean confirmCancellation
) {
}
