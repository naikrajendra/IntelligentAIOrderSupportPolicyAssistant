package com.example.ordersupport.support;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class McpToolDispatcher {

    private final OrderMcpToolService orderMcpToolService;

    public McpToolDispatcher(OrderMcpToolService orderMcpToolService) {
        this.orderMcpToolService = orderMcpToolService;
    }

    public McpRpcResponse dispatch(McpRpcRequest request) {
        if (request == null || request.method() == null || request.method().isBlank()) {
            return McpRpcResponse.failure(null, -32600, "Invalid request");
        }

        return switch (request.method()) {
            case "tools/list" -> McpRpcResponse.success(request.id(), Map.of("tools", buildToolList()));
            case "tools/call" -> handleToolCall(request);
            default -> McpRpcResponse.failure(request.id(), -32601, "Method not found");
        };
    }

    private McpRpcResponse handleToolCall(McpRpcRequest request) {
        Map<String, Object> params = request.params() == null ? Map.of() : request.params();
        String name = asString(params.get("name"));
        @SuppressWarnings("unchecked")
        Map<String, Object> arguments = params.get("arguments") instanceof Map<?, ?>
                ? (Map<String, Object>) params.get("arguments")
                : Map.of();

        if (name == null || name.isBlank()) {
            return McpRpcResponse.failure(request.id(), -32602, "Missing tool name");
        }

        return switch (name) {
            case "getOrderStatus" -> {
                String id = asString(arguments.get("id"));
                if (id == null || id.isBlank()) {
                    yield McpRpcResponse.failure(request.id(), -32602, "Missing id for getOrderStatus");
                }
                yield McpRpcResponse.success(request.id(), wrapToolResult(orderMcpToolService.getOrderStatus(id)));
            }
            case "cancelOrder" -> {
                String id = asString(arguments.get("id"));
                if (id == null || id.isBlank()) {
                    yield McpRpcResponse.failure(request.id(), -32602, "Missing id for cancelOrder");
                }
                yield McpRpcResponse.success(request.id(), wrapToolResult(orderMcpToolService.cancelOrder(id)));
            }
            default -> McpRpcResponse.failure(request.id(), -32601, "Unknown tool: " + name);
        };
    }

    private List<Map<String, Object>> buildToolList() {
        return List.of(
                Map.of(
                        "name", "getOrderStatus",
                        "description", "Get current status and shipment details for an order by id",
                        "inputSchema", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "id", Map.of("type", "string", "description", "Order ID")
                                ),
                                "required", List.of("id")
                        )
                ),
                Map.of(
                        "name", "cancelOrder",
                        "description", "Cancel an order by id when eligible",
                        "inputSchema", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "id", Map.of("type", "string", "description", "Order ID")
                                ),
                                "required", List.of("id")
                        )
                )
        );
    }

    private Map<String, Object> wrapToolResult(Map<String, Object> structuredContent) {
        return Map.of(
                "content", List.of(Map.of(
                        "type", "text",
                        "text", structuredContent.toString()
                )),
                "structuredContent", structuredContent
        );
    }

    private String asString(Object value) {
        return value instanceof String s ? s : null;
    }
}
