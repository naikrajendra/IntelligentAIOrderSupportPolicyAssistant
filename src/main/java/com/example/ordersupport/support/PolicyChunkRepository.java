package com.example.ordersupport.support;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface PolicyChunkRepository extends MongoRepository<PolicyChunkDocument, String> {
}
