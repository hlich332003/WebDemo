package com.mycompany.myapp.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis Cache Configuration for API Response Caching
 */
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    // ==================== CACHE NAMES ====================
    public static final String PRODUCT_CACHE = "products";
    public static final String PRODUCT_LIST_CACHE = "productList";
    public static final String CATEGORY_CACHE = "categories";
    public static final String CATEGORY_LIST_CACHE = "categoryList";
    public static final String USER_CACHE = "users";
    public static final String ORDER_CACHE = "orders";
    public static final String CART_CACHE = "carts";
    public static final String STATS_CACHE = "stats";

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultCacheConfiguration())
            .withInitialCacheConfigurations(cacheConfigurations())
            .transactionAware()
            .build();
    }

    /**
     * Default cache configuration
     * TTL: 10 minutes
     */
    private RedisCacheConfiguration defaultCacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
            .disableCachingNullValues();
    }

    /**
     * Custom cache configurations for different cache regions
     */
    private Map<String, RedisCacheConfiguration> cacheConfigurations() {
        Map<String, RedisCacheConfiguration> configs = new HashMap<>();

        // Products cache - 15 minutes (products don't change frequently)
        configs.put(PRODUCT_CACHE, defaultCacheConfiguration().entryTtl(Duration.ofMinutes(15)));

        // Product list cache - 5 minutes (lists change more frequently)
        configs.put(PRODUCT_LIST_CACHE, defaultCacheConfiguration().entryTtl(Duration.ofMinutes(5)));

        // Categories cache - 30 minutes (categories are very stable)
        configs.put(CATEGORY_CACHE, defaultCacheConfiguration().entryTtl(Duration.ofMinutes(30)));

        configs.put(CATEGORY_LIST_CACHE, defaultCacheConfiguration().entryTtl(Duration.ofMinutes(30)));

        // Users cache - 10 minutes
        configs.put(USER_CACHE, defaultCacheConfiguration().entryTtl(Duration.ofMinutes(10)));

        // Orders cache - 5 minutes (orders can be updated)
        configs.put(ORDER_CACHE, defaultCacheConfiguration().entryTtl(Duration.ofMinutes(5)));

        // Carts cache - 1 hour (session-based)
        configs.put(CART_CACHE, defaultCacheConfiguration().entryTtl(Duration.ofHours(1)));

        // Statistics cache - 1 minute (real-time stats)
        configs.put(STATS_CACHE, defaultCacheConfiguration().entryTtl(Duration.ofMinutes(1)));

        return configs;
    }

    /**
     * Custom key generator for better cache keys
     * Format: className.methodName:param1,param2
     */
    @Bean
    @Override
    public KeyGenerator keyGenerator() {
        return (target, method, params) -> {
            StringBuilder sb = new StringBuilder();
            sb.append(target.getClass().getSimpleName());
            sb.append(".");
            sb.append(method.getName());
            sb.append(":");

            for (Object param : params) {
                if (param != null) {
                    sb.append(param.toString());
                    sb.append(",");
                }
            }

            // Remove last comma
            if (sb.charAt(sb.length() - 1) == ',') {
                sb.deleteCharAt(sb.length() - 1);
            }

            return sb.toString();
        };
    }

    /**
     * Error handler for cache operations
     * Don't fail if cache is down
     */
    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new SimpleCacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                // Log but don't throw - fallback to database
                System.err.println("Cache GET error: " + exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, org.springframework.cache.Cache cache, Object key, Object value) {
                // Log but don't throw
                System.err.println("Cache PUT error: " + exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                // Log but don't throw
                System.err.println("Cache EVICT error: " + exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, org.springframework.cache.Cache cache) {
                // Log but don't throw
                System.err.println("Cache CLEAR error: " + exception.getMessage());
            }
        };
    }
}
