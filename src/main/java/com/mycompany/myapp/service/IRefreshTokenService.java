package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.RefreshToken;
import com.mycompany.myapp.domain.User;
import java.util.Optional;

/**
 * Service Interface cho Refresh Token operations
 * Follow Interface Segregation Principle
 */
public interface IRefreshTokenService {
    /**
     * Tìm refresh token by token string
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Tạo refresh token mới cho user
     */
    RefreshToken createRefreshToken(User user);

    /**
     * Verify token chưa expire
     */
    RefreshToken verifyExpiration(RefreshToken token);

    /**
     * Xóa refresh token by user ID
     */
    void deleteByUserId(Long userId);

    /**
     * Xóa refresh token by token string
     */
    void deleteByToken(String token);
}
