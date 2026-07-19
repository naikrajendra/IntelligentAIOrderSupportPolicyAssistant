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
        boolean cancellationRequested = isCancellationIntent(safeQuestion);
        boolean explicitConfirmationProvided = Boolean.TRUE.equals(request.confirmCancellation())
                || hasExplicitCancellationConfirmation(safeQuestion);

        Map<String, Object> statusToolResult = internalMcpToolClient.getOrderStatus(safeOrderId);
        String orderSnapshot = formatOrderToolResult(statusToolResult);

        String cancelToolResult = "No cancellation requested.";
        if (cancellationRequested && explicitConfirmationProvided) {
            Map<String, Object> cancellation = internalMcpToolClient.cancelOrder(safeOrderId);
            cancelToolResult = cancellation.toString();
        } else if (cancellationRequested) {
            cancelToolResult = "Cancellation not executed. Explicit confirmation is required before any cancellation action.";
        }

        String policyContext = policyChunkSearchService.fetchPolicyContext(safeQuestion);

        String prompt = """
            You are the Order Support Assistant for business teams.

                Goals:
            1) Help business users handle order inquiries quickly.
            2) Use order details to provide accurate updates and decisions.
            3) Apply company policies and return rules when recommending next actions.

                Important rules:
                - If policy details are missing, say what is missing.
                - If order data is incomplete, ask for the missing fields.
                - Be explicit when recommending refunds, vouchers, or escalation.
                - Never execute cancellation unless explicit confirmation is provided.
                - For cancellation requests without explicit confirmation, check policies and ask for confirmation first.
            - Write in plain business language for non-technical users.
            - Avoid engineering jargon, API references, or internal implementation details.
            - Keep the response concise, practical, and action-oriented.

                Context:
                customerId: %s
                orderId: %s
                customerQuestion: %s
                explicitConfirmationProvided: %s
                orderStatusToolResult: %s
                orderCancelToolResult: %s
                policyContextChunks: %s
                """.formatted(
                safeCustomerId,
                safeOrderId,
                safeQuestion,
                explicitConfirmationProvided,
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

    private boolean isCancellationIntent(String question) {
        String normalized = question.toLowerCase();
        return normalized.contains("cancel") || normalized.contains("cancellation");
    }

    private boolean hasExplicitCancellationConfirmation(String question) {
        String normalized = question.toLowerCase();
        return normalized.contains("confirm cancellation")
                || normalized.contains("i confirm")
                || normalized.contains("proceed with cancellation")
                || normalized.contains("yes cancel")
                || normalized.contains("cancel it now");
    }

    private String formatOrderToolResult(Map<String, Object> toolResult) {
        if (toolResult == null || toolResult.isEmpty()) {
            return "MCP getOrderStatus tool returned no data.";
        }
        return toolResult.toString();
    }
}
