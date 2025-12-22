package com.mycompany.myapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;

/**
 * OAuth2 Client Configuration for Google and Facebook login
 */
@Configuration
public class OAuth2ClientConfig {

    /**
     * Configure OAuth2 Client Registrations
     *
     * To enable, add these properties to application-dev.yml:
     *
     * spring:
     *   security:
     *     oauth2:
     *       client:
     *         registration:
     *           google:
     *             client-id: YOUR_GOOGLE_CLIENT_ID
     *             client-secret: YOUR_GOOGLE_CLIENT_SECRET
     *             scope: profile, email
     *           facebook:
     *             client-id: YOUR_FACEBOOK_APP_ID
     *             client-secret: YOUR_FACEBOOK_APP_SECRET
     *             scope: public_profile, email
     */

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        return new InMemoryClientRegistrationRepository(
            googleClientRegistration(),
            facebookClientRegistration()
        );
    }

    private ClientRegistration googleClientRegistration() {
        return ClientRegistration.withRegistrationId("google")
            .clientId("YOUR_GOOGLE_CLIENT_ID") // Replace with actual value or use ${spring.security.oauth2.client.registration.google.client-id}
            .clientSecret("YOUR_GOOGLE_CLIENT_SECRET")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
            .scope("openid", "profile", "email")
            .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
            .tokenUri("https://www.googleapis.com/oauth2/v4/token")
            .userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
            .userNameAttributeName(IdTokenClaimNames.SUB)
            .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
            .clientName("Google")
            .build();
    }

    private ClientRegistration facebookClientRegistration() {
        return ClientRegistration.withRegistrationId("facebook")
            .clientId("YOUR_FACEBOOK_APP_ID")
            .clientSecret("YOUR_FACEBOOK_APP_SECRET")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
            .scope("public_profile", "email")
            .authorizationUri("https://www.facebook.com/v12.0/dialog/oauth")
            .tokenUri("https://graph.facebook.com/v12.0/oauth/access_token")
            .userInfoUri("https://graph.facebook.com/me?fields=id,name,email")
            .userNameAttributeName("id")
            .clientName("Facebook")
            .build();
    }
}

