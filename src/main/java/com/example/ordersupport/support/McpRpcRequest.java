package com.example.ordersupport.support;

import java.util.Map;

public record McpRpcRequest(
        String jsonrpc,
        String id,
        String method,
        Map<String, Object> params
) {
}
