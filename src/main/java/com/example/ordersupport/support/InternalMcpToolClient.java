package com.example.ordersupport.support;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class InternalMcpToolClient {

    private final McpToolDispatcher dispatcher;

    public InternalMcpToolClient(McpToolDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public Map<String, Object> getOrderStatus(String id) {
        return callTool("getOrderStatus", Map.of("id", id));
    }

    public Map<String, Object> cancelOrder(String id) {
        return callTool("cancelOrder", Map.of("id", id));
    }

    private Map<String, Object> callTool(String name, Map<String, Object> arguments) {
        McpRpcRequest request = new McpRpcRequest(
                "2.0",
                "internal-chatbot",
                "tools/call",
                Map.of(
                        "name", name,
                        "arguments", arguments
                )
        );

        McpRpcResponse response = dispatcher.dispatch(request);
        if (response.error() != null) {
            return Map.of(
                    "success", false,
                    "message", response.error().message()
            );
        }

        if (!(response.result() instanceof Map<?, ?> resultMap)) {
            return Map.of(
                    "success", false,
                    "message", "Unexpected MCP result format"
            );
        }

        Object structuredContent = resultMap.get("structuredContent");
        if (structuredContent instanceof Map<?, ?> toolResult) {
            @SuppressWarnings("unchecked")
            Map<String, Object> typed = (Map<String, Object>) toolResult;
            return typed;
        }

        return Map.of(
                "success", false,
                "message", "Missing structuredContent in MCP result"
        );
    }
}
