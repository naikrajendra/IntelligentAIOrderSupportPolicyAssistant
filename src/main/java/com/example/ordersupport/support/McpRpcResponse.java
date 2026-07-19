package com.example.ordersupport.support;

public record McpRpcResponse(
        String jsonrpc,
        String id,
        Object result,
        McpRpcError error
) {

    public static McpRpcResponse success(String id, Object result) {
        return new McpRpcResponse("2.0", id, result, null);
    }

    public static McpRpcResponse failure(String id, int code, String message) {
        return new McpRpcResponse("2.0", id, null, new McpRpcError(code, message));
    }
}
