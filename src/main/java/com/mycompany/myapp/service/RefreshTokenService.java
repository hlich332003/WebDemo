package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.RefreshToken;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.repository.RefreshTokenRepository;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.service.errors.TokenRefreshException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service để quản lý Refresh Token
 * Implements IRefreshTokenService interface (SOLID principles)
 */
@Service
@Transactional
public class RefreshTokenService implements IRefreshTokenService {

    // Refresh token validity: 30 days (2592000 seconds)
    private static final Long REFRESH_TOKEN_VALIDITY_SECONDS = 2592000L;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, UserRepository userRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Override
    public RefreshToken createRefreshToken(User user) {
        Optional<RefreshToken> existingToken = refreshTokenRepository.findByUser(user);
        if (existingToken.isPresent()) {
            // Nếu đã có refresh token cho user này, cập nhật nó
            RefreshToken token = existingToken.get();
            token.setToken(UUID.randomUUID().toString());
            token.setExpiryDate(Instant.now().plusSeconds(REFRESH_TOKEN_VALIDITY_SECONDS));
            return refreshTokenRepository.save(token);
        } else {
            // Nếu chưa có, tạo mới
            RefreshToken refreshToken = new RefreshToken();
            refreshToken.setUser(user);
            refreshToken.setExpiryDate(Instant.now().plusSeconds(REFRESH_TOKEN_VALIDITY_SECONDS));
            refreshToken.setToken(UUID.randomUUID().toString());
            return refreshTokenRepository.save(refreshToken);
        }
    }

    @Override
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException(token.getToken(), "Refresh token was expired. Please make a new signin request");
        }
        return token;
    }

    @Override
    public void deleteByUserId(Long userId) {
        refreshTokenRepository.deleteByUser(userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found")));
    }

    @Override
    public void deleteByToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(refreshTokenRepository::delete);
    }
}
