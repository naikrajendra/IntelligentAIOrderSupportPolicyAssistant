package com.example.ordersupport.support;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface ConversationHistoryRepository extends MongoRepository<ConversationHistory, String> {
}
