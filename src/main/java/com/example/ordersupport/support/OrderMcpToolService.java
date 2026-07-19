package com.example.ordersupport.support;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OrderMcpToolService {

    private final OrderRepository orderRepository;

    public OrderMcpToolService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Map<String, Object> getOrderStatus(String id) {
        return orderRepository.findFirstByOrderId(id)
                .<Map<String, Object>>map(order -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("found", true);
                    result.put("orderId", order.orderId());
                    result.put("customerId", order.customerId());
                    result.put("trackingNumber", order.trackingNumber());
                    result.put("purchaseDate", order.purchaseDate());
                    result.put("expectedDeliveryDate", order.expectedDeliveryDate());
                    result.put("deliveredDate", order.deliveredDate());
                    result.put("orderTotal", order.orderTotal());
                    result.put("shippingFee", order.shippingFee());
                    result.put("status", order.status());
                    result.put("asOf", LocalDateTime.now().toString());
                    return result;
                })
                .orElseGet(() -> Map.of(
                        "found", false,
                        "orderId", id,
                        "message", "Order not found"
                ));
    }

    public Map<String, Object> cancelOrder(String id) {
        return orderRepository.findFirstByOrderId(id)
                .<Map<String, Object>>map(order -> {
                    String status = order.status() == null ? "UNKNOWN" : order.status().toUpperCase();

                    if ("DELIVERED".equals(status)) {
                        return Map.of(
                                "success", false,
                                "orderId", id,
                                "status", status,
                                "message", "Delivered order cannot be canceled"
                        );
                    }

                    if ("CANCELLED".equals(status)) {
                        return Map.of(
                                "success", true,
                                "orderId", id,
                                "status", status,
                                "message", "Order is already canceled"
                        );
                    }

                    OrderSnapshot canceledOrder = new OrderSnapshot(
                            order.id(),
                            order.orderId(),
                            order.customerId(),
                            order.trackingNumber(),
                            order.purchaseDate(),
                            order.expectedDeliveryDate(),
                            order.deliveredDate(),
                            order.orderTotal(),
                            order.shippingFee(),
                            "CANCELLED"
                    );

                    orderRepository.save(canceledOrder);

                    return Map.of(
                            "success", true,
                            "orderId", id,
                            "previousStatus", status,
                            "newStatus", "CANCELLED",
                            "message", "Order canceled successfully"
                    );
                })
                .orElseGet(() -> Map.of(
                        "success", false,
                        "orderId", id,
                        "message", "Order not found"
                ));
    }
}
