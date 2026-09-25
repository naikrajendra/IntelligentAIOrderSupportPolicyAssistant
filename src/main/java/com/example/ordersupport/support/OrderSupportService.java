package com.example.ordersupport.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class OrderSupportService {

    private static final long USER_TOKEN_QUOTA = 50_000L;
    private static final long MINIMUM_ESTIMATED_TOKENS_PER_REQUEST = 1_000L;

    private final ChatClient chatClient;
    private final InternalMcpToolClient internalMcpToolClient;
    private final PolicyChunkSearchService policyChunkSearchService;
    private final RedisTokenRateLimiter redisTokenRateLimiter;
    private final ConversationHistoryService conversationHistoryService;

    public OrderSupportService(
            ChatClient.Builder chatClientBuilder,
            InternalMcpToolClient internalMcpToolClient,
            PolicyChunkSearchService policyChunkSearchService,
            RedisTokenRateLimiter redisTokenRateLimiter,
            ConversationHistoryService conversationHistoryService
    ) {
        this.chatClient = chatClientBuilder.build();
        this.internalMcpToolClient = internalMcpToolClient;
        this.policyChunkSearchService = policyChunkSearchService;
        this.redisTokenRateLimiter = redisTokenRateLimiter;
        this.conversationHistoryService = conversationHistoryService;
    }

    public String generatePolicyAwareAnswer(OrderSupportRequest request) {
        String safeCustomerId = safe(request.customerId());
        String safeOrderId = safe(request.orderId());
        String safeQuestion = safe(request.question());
        boolean cancellationRequested = isCancellationIntent(safeQuestion);
        boolean explicitConfirmationProvided = Boolean.TRUE.equals(request.confirmCancellation())
                || hasExplicitCancellationConfirmation(safeQuestion);

        if (!redisTokenRateLimiter.tryConsume(safeCustomerId, MINIMUM_ESTIMATED_TOKENS_PER_REQUEST)) {
            return "Your usage quota is exhausted for this hour. Please try again later. "
                    + "The limit is 50,000 tokens per customer per 1 hour window.";
        }

        conversationHistoryService.addUserMessage(safeCustomerId, safeQuestion);

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
        List<ChatMessage> userHistory = conversationHistoryService.getHistory(safeCustomerId);
        String conversationContext = buildOptimizedConversationContext(userHistory);

        String prompt = """
            You are the Order Support Assistant for business operations and IT operations teams.

                Goals:
            1) Help business users handle order inquiries quickly.
            2) Use order details to provide accurate updates and decisions.
            3) Apply company order policies and return rules when recommending next actions.

                Important rules:
                - If policy details are missing, say what is missing.
                - If order data is incomplete, ask for the missing fields.
                - Be explicit when recommending refunds, vouchers, or escalation.
                - Never execute cancellation unless explicit confirmation is provided.
                - For cancellation requests without explicit confirmation, check policies and ask for confirmation first.
                - Use the earlier conversation summary to maintain continuity without repeating everything.
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
                optimizedConversationContext:
                %s
                """.formatted(
                safeCustomerId,
                safeOrderId,
                safeQuestion,
                explicitConfirmationProvided,
                orderSnapshot,
                cancelToolResult,
                policyContext,
                conversationContext
        );

        String answer = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        conversationHistoryService.addAssistantMessage(safeCustomerId, answer);
        return answer;
    }

    static String buildOptimizedConversationContext(List<ChatMessage> history) {
        if (history == null || history.isEmpty()) {
            return "No prior conversation history.";
        }

        List<ChatMessage> recentMessages = history.size() <= 6
                ? history
                : history.subList(Math.max(0, history.size() - 6), history.size());

        List<String> earlierTopics = new ArrayList<>();
        if (history.size() > 6) {
            for (ChatMessage message : history.subList(0, Math.max(0, history.size() - 6))) {
                String text = message.content() == null ? "" : message.content().trim();
                if (!text.isBlank()) {
                    earlierTopics.add(message.role() + ": " + text);
                }
            }
        }

        String earlierSummary = earlierTopics.isEmpty() ? "No earlier conversation summary." :
                earlierTopics.stream().limit(4).collect(Collectors.joining(" | "));

        String recentConversation = recentMessages.stream()
                .filter(message -> message.content() != null && !message.content().isBlank())
                .map(message -> message.role() + ": " + message.content())
                .collect(Collectors.joining("\n"));

        return "Earlier conversation summary: " + earlierSummary + "\nRecent conversation:\n" + recentConversation;
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
