package com.mycompany.myapp.security.oauth2;

import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.web.rest.AuthenticateController;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Optional;

/**
 * Handler for successful OAuth2 authentication
 */
@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final Logger log = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);
    private final AuthenticateController authenticateController;
    private final UserRepository userRepository;

    public OAuth2AuthenticationSuccessHandler(AuthenticateController authenticateController, UserRepository userRepository) {
        this.authenticateController = authenticateController;
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
            String email = oauth2User.getAttribute("email");

            log.info("OAuth2 login successful for email: {}", email);

            // Find or create user
            Optional<User> userOptional = userRepository.findOneByEmailIgnoreCase(email);

            if (userOptional.isPresent()) {

                // Generate JWT token
                String jwt = authenticateController.createToken(authentication, false);

                // Redirect to frontend with token
                String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:4200")
                    .queryParam("token", jwt)
                    .build().toUriString();

                log.info("Redirecting to: {}", targetUrl);
                getRedirectStrategy().sendRedirect(request, response, targetUrl);
            } else {
                log.warn("User not found for OAuth2 email: {}", email);
                // Redirect to registration page
                String targetUrl = "http://localhost:4200/register?email=" + email;
                getRedirectStrategy().sendRedirect(request, response, targetUrl);
            }
        } else {
            super.onAuthenticationSuccess(request, response, authentication);
        }
    }
}

