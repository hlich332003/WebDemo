package com.mycompany.myapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Redis Publisher for chat messages - enables multi-instance scaling
 */
@Service
public class ChatRedisPublisher {

    private static final Logger log = LoggerFactory.getLogger(ChatRedisPublisher.class);
    private static final String CHAT_CHANNEL_PREFIX = "chat-room-";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public ChatRedisPublisher(@Qualifier("chatRedisTemplate") RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publish message to Redis channel for a specific conversation
     *
     * @param conversationId The chat conversation ID
     * @param message The message payload (will be serialized to JSON)
     */
    public void publishChatMessage(Long conversationId, Object message) {
        try {
            String channel = CHAT_CHANNEL_PREFIX + conversationId;
            String jsonPayload = objectMapper.writeValueAsString(message);

            redisTemplate.convertAndSend(channel, jsonPayload);

            log.info("📤 [REDIS PUB] Published to channel: {} | Payload size: {} bytes", channel, jsonPayload.length());
        } catch (Exception e) {
            log.error("❌ Failed to publish message to Redis for conversation: {}", conversationId, e);
        }
    }
}
