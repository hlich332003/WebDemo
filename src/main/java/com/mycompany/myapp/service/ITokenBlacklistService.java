package com.mycompany.myapp.service;

import java.time.Instant;

/**
 * Service Interface cho Token Blacklist operations
 * Follow Interface Segregation Principle
 */
public interface ITokenBlacklistService {
    /**
     * Thêm token vào blacklist với TTL
     */
    void blacklistToken(String token, Instant expiryDate);

    /**
     * Kiểm tra token có bị blacklist không
     */
    boolean isBlacklisted(String token);
}
