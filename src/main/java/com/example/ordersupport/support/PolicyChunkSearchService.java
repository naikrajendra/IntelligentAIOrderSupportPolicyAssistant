package com.example.ordersupport.support;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class PolicyChunkSearchService {

    private final PolicyChunkRepository policyChunkRepository;

    public PolicyChunkSearchService(PolicyChunkRepository policyChunkRepository) {
        this.policyChunkRepository = policyChunkRepository;
    }

    public String fetchPolicyContext(String question) {
        List<String> chunks = policyChunkRepository.findAll().stream()
                .map(PolicyChunkDocument::chunkText)
                .toList();

        if (chunks.isEmpty()) {
            return "No relevant policy chunks found in policy chunk database.";
        }

        Set<String> queryTerms = tokenize(question);

        List<String> topChunks = chunks.stream()
                .sorted((a, b) -> Integer.compare(scoreChunk(b, queryTerms), scoreChunk(a, queryTerms)))
                .limit(4)
                .toList();

        return topChunks.stream()
                .collect(Collectors.joining("\n---\n"));
    }

    private int scoreChunk(String chunk, Set<String> queryTerms) {
        if (queryTerms.isEmpty()) {
            return 0;
        }

        String normalizedChunk = chunk.toLowerCase(Locale.ROOT);
        int score = 0;
        for (String term : queryTerms) {
            if (normalizedChunk.contains(term)) {
                score++;
            }
        }
        return score;
    }

    private Set<String> tokenize(String text) {
        String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT);
        return Arrays.stream(normalized.split("[^a-z0-9]+"))
                .filter(token -> !token.isBlank())
                .collect(Collectors.toSet());
    }
}
