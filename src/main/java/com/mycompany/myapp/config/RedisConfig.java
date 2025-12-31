package com.mycompany.myapp.config;

import com.mycompany.myapp.service.ChatRedisSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis configuration for caching, token blacklist, and chat pub/sub.
 * All beans only created when Redis connection is available.
 */
@Configuration
public class RedisConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);
    private static final String CHAT_CHANNEL_PATTERN = "chat-room-*";

    /**
     * RedisTemplate for chat messages pub/sub (String keys & values)
     * Named bean to avoid conflict with CacheConfiguration's redisTemplate
     * Only created if RedisConnectionFactory is available
     */
    @Bean(name = "chatRedisTemplate")
    public RedisTemplate<String, String> chatRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);

        template.afterPropertiesSet();
        log.info("✅ [REDIS CONFIG] Chat RedisTemplate initialized");
        return template;
    }

    /**
     * Message listener adapter for ChatRedisSubscriber
     * Only created if RedisConnectionFactory is available
     */
    @Bean
    public MessageListenerAdapter chatMessageListenerAdapter(ChatRedisSubscriber subscriber) {
        MessageListenerAdapter adapter = new MessageListenerAdapter(subscriber, "onMessage");
        log.info("✅ [REDIS CONFIG] Message listener adapter created");
        return adapter;
    }

    /**
     * Configure RedisMessageListenerContainer for chat pub/sub
     * Only created if RedisConnectionFactory is available
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
        RedisConnectionFactory connectionFactory,
        MessageListenerAdapter chatMessageListenerAdapter
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // Subscribe to all chat room channels (pattern: chat-room-*)
        container.addMessageListener(chatMessageListenerAdapter, new PatternTopic(CHAT_CHANNEL_PATTERN));

        log.info("✅ [REDIS CONFIG] Message listener container initialized - subscribed to: {}", CHAT_CHANNEL_PATTERN);
        return container;
    }

    /**
     * Configure RedisTemplate for Token Blacklist.
     * Used for JWT token blacklisting.
     * Only created if RedisConnectionFactory is available
     *
     * @param connectionFactory Redis connection factory
     * @return configured RedisTemplate for String operations
     */
    @Bean(name = "tokenBlacklistRedisTemplate")
    public RedisTemplate<String, String> tokenBlacklistRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);

        template.afterPropertiesSet();
        log.info("✅ [REDIS CONFIG] Token Blacklist RedisTemplate initialized");
        return template;
    }
}
