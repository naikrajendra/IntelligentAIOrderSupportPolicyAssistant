package com.example.ordersupport.support;

public record McpRpcError(
        int code,
        String message
) {
}
