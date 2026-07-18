package com.example.ordersupport.support;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class OrderSupportService {

    private final ChatClient chatClient;
    private final OrderRepository orderRepository;
    private final PolicyChunkSearchService policyChunkSearchService;

    public OrderSupportService(
            ChatClient.Builder chatClientBuilder,
            OrderRepository orderRepository,
            PolicyChunkSearchService policyChunkSearchService
    ) {
        this.chatClient = chatClientBuilder.build();
        this.orderRepository = orderRepository;
        this.policyChunkSearchService = policyChunkSearchService;
    }

    public String generatePolicyAwareAnswer(OrderSupportRequest request) {
        String safeCustomerId = safe(request.customerId());
        String safeOrderId = safe(request.orderId());
        String safeQuestion = safe(request.question());

        String orderSnapshot = orderRepository.findByCustomerIdAndOrderId(safeCustomerId, safeOrderId)
                .map(this::formatOrderSnapshot)
            .orElse("Order not found in transactional database for provided customerId + orderId.");

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
                orderSnapshot: %s
                policyContextChunks: %s
                """.formatted(
                safeCustomerId,
                safeOrderId,
                safeQuestion,
                orderSnapshot,
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

    private String formatOrderSnapshot(OrderSnapshot snapshot) {
        return """
            orderId=%s,
                customerId=%s,
                trackingNumber=%s,
                purchaseDate=%s,
                expectedDeliveryDate=%s,
                deliveredDate=%s,
                orderTotal=%s,
                shippingFee=%s,
                status=%s
                """.formatted(
                snapshot.orderId(),
                snapshot.customerId(),
                snapshot.trackingNumber(),
                snapshot.purchaseDate(),
                snapshot.expectedDeliveryDate(),
                snapshot.deliveredDate(),
                snapshot.orderTotal(),
                snapshot.shippingFee(),
                snapshot.status()
        );
    }
}
