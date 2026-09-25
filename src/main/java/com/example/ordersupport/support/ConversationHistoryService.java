package com.example.ordersupport.support;

import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ConversationHistoryService {

    private final RedisChatHistoryService redisChatHistoryService;
    private final ConversationHistoryRepository conversationHistoryRepository;

    public ConversationHistoryService(
            RedisChatHistoryService redisChatHistoryService,
            ConversationHistoryRepository conversationHistoryRepository
    ) {
        this.redisChatHistoryService = redisChatHistoryService;
        this.conversationHistoryRepository = conversationHistoryRepository;
    }

    public void addUserMessage(String userId, String content) {
        redisChatHistoryService.appendMessage(userId, new ChatMessage("user", content, Instant.now()));
    }

    public void addAssistantMessage(String userId, String content) {
        redisChatHistoryService.appendMessage(userId, new ChatMessage("assistant", content, Instant.now()));
    }

    public List<ChatMessage> getHistory(String userId) {
        return redisChatHistoryService.getMessages(userId);
    }

    public boolean persistConversationToMongo(String userId) {
        List<ChatMessage> messages = redisChatHistoryService.getMessages(userId);
        if (messages.isEmpty()) {
            return false;
        }

        ConversationHistory history = new ConversationHistory(userId, Instant.now());
        history.setMessages(messages);
        history.setUpdatedAt(Instant.now());
        conversationHistoryRepository.save(history);
        redisChatHistoryService.clear(userId);
        return true;
    }
}
