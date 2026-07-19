package com.example.ordersupport.support;

import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class OrderSupportService {

    private final ChatClient chatClient;
    private final InternalMcpToolClient internalMcpToolClient;
    private final PolicyChunkSearchService policyChunkSearchService;

    public OrderSupportService(
            ChatClient.Builder chatClientBuilder,
            InternalMcpToolClient internalMcpToolClient,
            PolicyChunkSearchService policyChunkSearchService
    ) {
        this.chatClient = chatClientBuilder.build();
        this.internalMcpToolClient = internalMcpToolClient;
        this.policyChunkSearchService = policyChunkSearchService;
    }

    public String generatePolicyAwareAnswer(OrderSupportRequest request) {
        String safeCustomerId = safe(request.customerId());
        String safeOrderId = safe(request.orderId());
        String safeQuestion = safe(request.question());

        Map<String, Object> statusToolResult = internalMcpToolClient.getOrderStatus(safeOrderId);
        String orderSnapshot = formatOrderToolResult(statusToolResult);

        String cancelToolResult = "No cancellation requested.";
        if (shouldAttemptCancel(safeQuestion)) {
            Map<String, Object> cancellation = internalMcpToolClient.cancelOrder(safeOrderId);
            cancelToolResult = cancellation.toString();
        }

        String policyContext = policyChunkSearchService.fetchPolicyContext(safeQuestion);

        String prompt = """
                You are an Intelligent Order Support and Policy Assistant.

                Goals:
                1) Help with complex order inquiries.
                2) Use order details to give accurate support.
                3) Apply company policies and return rules when recommending next actions.

                Important rules:
                - If policy details are missing, say what is missing.
                - If order data is incomplete, ask for the missing fields.
                - Be explicit when recommending refunds, vouchers, or escalation.
                - Keep the response concise and customer-friendly.

                Context:
                customerId: %s
                orderId: %s
                customerQuestion: %s
                orderStatusToolResult: %s
                orderCancelToolResult: %s
                policyContextChunks: %s
                """.formatted(
                safeCustomerId,
                safeOrderId,
                safeQuestion,
                orderSnapshot,
                cancelToolResult,
                policyContext
        );

        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }

    private boolean shouldAttemptCancel(String question) {
        String normalized = question.toLowerCase();
        return normalized.contains("cancel") || normalized.contains("cancellation");
    }

    private String formatOrderToolResult(Map<String, Object> toolResult) {
        if (toolResult == null || toolResult.isEmpty()) {
            return "MCP getOrderStatus tool returned no data.";
        }
        return toolResult.toString();
    }
}
