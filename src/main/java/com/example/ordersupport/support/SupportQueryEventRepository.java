package com.example.ordersupport.support;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SupportQueryEventRepository extends MongoRepository<SupportQueryEvent, String> {
    List<SupportQueryEvent> findByCustomerId(String customerId);
}
