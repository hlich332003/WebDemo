package com.mycompany.myapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

/**
 * Redis Subscriber for chat messages - receives messages from other instances
 */
@Service
public class ChatRedisSubscriber implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(ChatRedisSubscriber.class);

    private final SimpMessageSendingOperations messagingTemplate;
    private final ObjectMapper objectMapper;

    public ChatRedisSubscriber(SimpMessageSendingOperations messagingTemplate, ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel());
            String payload = new String(message.getBody());

            log.info("📥 [REDIS SUB] Received from channel: {} | Payload size: {} bytes", channel, payload.length());

            // Parse message payload
            @SuppressWarnings("unchecked")
            Map<String, Object> messageData = objectMapper.readValue(payload, Map.class);

            // Extract conversation ID from channel name (format: chat-room-123)
            Long conversationId = Long.parseLong(channel.substring(channel.lastIndexOf('-') + 1));

            // Broadcast to WebSocket subscribers
            String destination = "/topic/chat." + conversationId;
            messagingTemplate.convertAndSend(destination, messageData);

            log.info("📢 [WEBSOCKET] Forwarded Redis message to: {}", destination);
        } catch (Exception e) {
            log.error("❌ Failed to process Redis message", e);
        }
    }
}
