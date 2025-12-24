package com.mycompany.myapp.service.dto;

import java.time.Instant;

public class ChatResponseDTO {

    private Long conversationId;
    private String senderType;
    private String senderIdentifier;
    private String content;
    private Instant createdAt;
    private String type; // MESSAGE, SESSION_ENDED

    public ChatResponseDTO() {}

    public ChatResponseDTO(
        Long conversationId,
        String senderType,
        String senderIdentifier,
        String content,
        Instant createdAt,
        String type
    ) {
        this.conversationId = conversationId;
        this.senderType = senderType;
        this.senderIdentifier = senderIdentifier;
        this.content = content;
        this.createdAt = createdAt;
        this.type = type;
    }

    // Getters and Setters
    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public String getSenderType() {
        return senderType;
    }

    public void setSenderType(String senderType) {
        this.senderType = senderType;
    }

    public String getSenderIdentifier() {
        return senderIdentifier;
    }

    public void setSenderIdentifier(String senderIdentifier) {
        this.senderIdentifier = senderIdentifier;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
