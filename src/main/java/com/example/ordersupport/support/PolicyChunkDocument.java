package com.example.ordersupport.support;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "policy_chunks")
public record PolicyChunkDocument(
        @Id
        String id,
        String source,
        int chunkStart,
        int chunkEnd,
        String chunkText,
        Instant ingestedAt
) {
}
