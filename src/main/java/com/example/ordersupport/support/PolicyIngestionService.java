package com.example.ordersupport.support;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

@Service
public class PolicyIngestionService {

    private static final int CHUNK_SIZE = 1200;
    private static final int CHUNK_OVERLAP = 150;

    private final PolicyChunkRepository policyChunkRepository;

    public PolicyIngestionService(PolicyChunkRepository policyChunkRepository) {
        this.policyChunkRepository = policyChunkRepository;
    }

    public PolicyIngestionResponse ingestFromDirectory(Path directory) throws IOException {
        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            throw new IllegalArgumentException("Policy directory does not exist: " + directory);
        }

        List<PolicyChunkDocument> chunks = new ArrayList<>();
        int filesIngested = 0;
        Instant ingestedAt = Instant.now();

        try (Stream<Path> paths = Files.walk(directory, 1)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                String fileName = path.getFileName().toString().toLowerCase();
                if (!isSupportedPolicyFile(fileName)) {
                    continue;
                }

                String text = extractText(path, fileName);
                if (text.isBlank()) {
                    continue;
                }

                filesIngested++;
                chunks.addAll(splitIntoChunks(text, path.getFileName().toString(), ingestedAt));
            }
        }

        if (!chunks.isEmpty()) {
            // Replace current policy context with the latest ingestion snapshot.
            policyChunkRepository.deleteAll();
            policyChunkRepository.saveAll(chunks);
        }

        return new PolicyIngestionResponse(filesIngested, chunks.size());
    }

    private boolean isSupportedPolicyFile(String fileName) {
        return fileName.endsWith(".pdf") || fileName.endsWith(".txt") || fileName.endsWith(".md");
    }

    private String extractText(Path filePath, String fileName) throws IOException {
        if (fileName.endsWith(".pdf")) {
            return extractPdfText(filePath);
        }
        return Files.readString(filePath);
    }

    private String extractPdfText(Path pdfPath) throws IOException {
        try (PDDocument pdDocument = Loader.loadPDF(pdfPath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(pdDocument);
        }
    }

    private List<PolicyChunkDocument> splitIntoChunks(String text, String sourceFile, Instant ingestedAt) {
        List<PolicyChunkDocument> docs = new ArrayList<>();
        String normalized = text.replace("\r", "").trim();

        for (int start = 0; start < normalized.length(); start += (CHUNK_SIZE - CHUNK_OVERLAP)) {
            int end = Math.min(normalized.length(), start + CHUNK_SIZE);
            String chunk = normalized.substring(start, end).trim();
            if (chunk.isEmpty()) {
                continue;
            }

            docs.add(new PolicyChunkDocument(null, sourceFile, start, end, chunk, ingestedAt));

            if (end >= normalized.length()) {
                break;
            }
        }

        return docs;
    }
}
