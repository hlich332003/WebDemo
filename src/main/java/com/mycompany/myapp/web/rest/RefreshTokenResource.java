package com.mycompany.myapp.web.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mycompany.myapp.domain.RefreshToken;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.security.DomainUserDetailsService.UserWithId;
import com.mycompany.myapp.security.SecurityUtils;
import com.mycompany.myapp.service.RefreshTokenService;
import com.mycompany.myapp.service.errors.TokenRefreshException;
import com.mycompany.myapp.web.rest.AuthenticateController.JWTToken; // Import JWTToken class
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RefreshTokenResource {

    private static final Logger LOG = LoggerFactory.getLogger(RefreshTokenResource.class);

    private final JwtEncoder jwtEncoder;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;

    @Value("${jhipster.security.authentication.jwt.token-validity-in-seconds:0}")
    private long tokenValidityInSeconds;

    @Value("${jhipster.security.authentication.jwt.token-validity-in-seconds-for-remember-me:0}")
    private long tokenValidityInSecondsForRememberMe;

    public RefreshTokenResource(JwtEncoder jwtEncoder, RefreshTokenService refreshTokenService, UserRepository userRepository) {
        this.jwtEncoder = jwtEncoder;
        this.refreshTokenService = refreshTokenService;
        this.userRepository = userRepository;
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<JWTToken> refreshToken(@RequestBody TokenRefreshRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        return refreshTokenService
            .findByToken(requestRefreshToken)
            .map(refreshTokenService::verifyExpiration)
            .map(RefreshToken::getUser)
            .map(user -> {
                // Convert authorities từ Set<Authority> sang List<GrantedAuthority>
                java.util.List<org.springframework.security.core.GrantedAuthority> authorities = user
                    .getAuthorities()
                    .stream()
                    .map(authority -> new org.springframework.security.core.authority.SimpleGrantedAuthority(authority.getName()))
                    .collect(java.util.stream.Collectors.toList());

                // Tạo Authentication object từ user
                Authentication authentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                    new UserWithId(user.getLogin(), "", authorities, user.getId()),
                    null,
                    authorities
                );

                String newAccessToken = createToken(authentication, false);

                // Trả về response với token mới
                HttpHeaders httpHeaders = new HttpHeaders();
                httpHeaders.setBearerAuth(newAccessToken);
                return ResponseEntity.ok().headers(httpHeaders).body(new JWTToken(newAccessToken));
            })
            .orElseThrow(() -> new TokenRefreshException(requestRefreshToken, "Refresh token is not in database!"));
    }

    // Helper method to create a new Access Token (copied from AuthenticateController)
    private String createToken(Authentication authentication, boolean rememberMe) {
        String authorities = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.joining(" "));

        Instant now = Instant.now();
        Instant validity;
        if (rememberMe) {
            validity = now.plus(this.tokenValidityInSecondsForRememberMe, ChronoUnit.SECONDS);
        } else {
            validity = now.plus(this.tokenValidityInSeconds, ChronoUnit.SECONDS);
        }

        // @formatter:off
        JwtClaimsSet.Builder builder = JwtClaimsSet.builder()
            .issuedAt(now)
            .expiresAt(validity)
            .subject(authentication.getName())
            .claim(SecurityUtils.AUTHORITIES_CLAIM, authorities);
        if (authentication.getPrincipal() instanceof UserWithId user) {
            builder.claim(SecurityUtils.USER_ID_CLAIM, user.getId());
        }

        JwsHeader jwsHeader = JwsHeader.with(SecurityUtils.JWT_ALGORITHM).build();
        return this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, builder.build())).getTokenValue();
    }

    static class TokenRefreshRequest {

        private String refreshToken;

        @JsonProperty("refreshToken")
        public String getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }
    }
}
