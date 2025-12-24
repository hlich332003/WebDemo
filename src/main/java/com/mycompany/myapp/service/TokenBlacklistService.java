package com.mycompany.myapp.service;

import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Service để quản lý JWT token blacklist trong Redis
 * Dùng để vô hiệu hóa token khi user logout
 * Implements ITokenBlacklistService interface (SOLID principles)
 */
@Service
public class TokenBlacklistService implements ITokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "token:blacklist:";
    private final RedisTemplate<String, String> redisTemplate;

    public TokenBlacklistService(@Qualifier("tokenBlacklistRedisTemplate") RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Thêm token vào blacklist
     * Token sẽ tự động bị xóa khỏi Redis khi hết hạn (dùng Redis TTL)
     *
     * @param token JWT token cần blacklist
     * @param expiryDate Thời gian hết hạn của token
     */
    @Override
    public void blacklistToken(String token, Instant expiryDate) {
        String key = BLACKLIST_PREFIX + token;
        long ttl = Duration.between(Instant.now(), expiryDate).getSeconds();

        if (ttl > 0) {
            // Lưu vào Redis với TTL = thời gian còn lại đến khi token hết hạn
            // Redis sẽ tự động xóa key khi hết TTL
            redisTemplate.opsForValue().set(key, "blacklisted", Duration.ofSeconds(ttl));
        }
    }

    /**
     * Kiểm tra token có trong blacklist không
     *
     * @param token JWT token cần kiểm tra
     * @return true nếu token đã bị blacklist (logout), false nếu vẫn còn hợp lệ
     */
    @Override
    public boolean isBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
