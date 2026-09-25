package com.example.ordersupport.support;

public record OrderSupportResponse(
        String answer,
        Double confidenceScore,
        TokenUtilization tokenUtilization
) {
}
