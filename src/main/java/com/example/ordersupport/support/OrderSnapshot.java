package com.example.ordersupport.support;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "orders")
public record OrderSnapshot(
        @Id
        String id,
        String orderId,
        String customerId,
        String trackingNumber,
        LocalDate purchaseDate,
        LocalDate expectedDeliveryDate,
        LocalDate deliveredDate,
        BigDecimal orderTotal,
        BigDecimal shippingFee,
        String status
) {
}
