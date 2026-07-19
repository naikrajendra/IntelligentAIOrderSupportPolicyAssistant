package com.example.ordersupport.support;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OrderRepository extends MongoRepository<OrderSnapshot, String> {

    Optional<OrderSnapshot> findByCustomerIdAndOrderId(String customerId, String orderId);

    Optional<OrderSnapshot> findFirstByOrderId(String orderId);
}
