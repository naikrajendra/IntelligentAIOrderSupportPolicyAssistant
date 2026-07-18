package com.example.ordersupport.support;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OrderSeedService {

    private final OrderRepository orderRepository;

    public OrderSeedService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public int seedDefaultOrders() {
        List<OrderSnapshot> seedOrders = List.of(
                new OrderSnapshot(
                        null,
                        "O-1001",
                        "C-991",
                        "1Z-TRACK-901",
                        LocalDate.parse("2026-07-01"),
                        LocalDate.parse("2026-07-08"),
                        null,
                        new BigDecimal("149.99"),
                        new BigDecimal("9.99"),
                        "IN_TRANSIT"
                ),
                new OrderSnapshot(
                        null,
                        "O-1002",
                        "C-992",
                        "1Z-TRACK-902",
                        LocalDate.parse("2026-06-25"),
                        LocalDate.parse("2026-07-03"),
                        LocalDate.parse("2026-07-10"),
                        new BigDecimal("89.50"),
                        new BigDecimal("0.00"),
                        "DELIVERED"
                )
        );

        for (OrderSnapshot seedOrder : seedOrders) {
            orderRepository.findByCustomerIdAndOrderId(seedOrder.customerId(), seedOrder.orderId())
                    .ifPresentOrElse(
                            existing -> orderRepository.save(new OrderSnapshot(
                                    existing.id(),
                                    seedOrder.orderId(),
                                    seedOrder.customerId(),
                                    seedOrder.trackingNumber(),
                                    seedOrder.purchaseDate(),
                                    seedOrder.expectedDeliveryDate(),
                                    seedOrder.deliveredDate(),
                                    seedOrder.orderTotal(),
                                    seedOrder.shippingFee(),
                                    seedOrder.status()
                            )),
                            () -> orderRepository.save(seedOrder)
                    );
        }

        return seedOrders.size();
    }
}
