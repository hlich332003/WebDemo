package com.mycompany.myapp.config;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

import com.mycompany.myapp.security.AuthoritiesConstants;
import com.mycompany.myapp.security.jwt.JwtBlacklistFilter;
import com.mycompany.myapp.web.filter.SpaWebFilter;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;
import tech.jhipster.config.JHipsterConstants;
import tech.jhipster.config.JHipsterProperties;

@Configuration
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfiguration {

    private final Environment env;
    private final JHipsterProperties jHipsterProperties;
    private final JwtBlacklistFilter jwtBlacklistFilter;
    private final com.mycompany.myapp.repository.UserRepository userRepository;

    public SecurityConfiguration(
        Environment env,
        JHipsterProperties jHipsterProperties,
        JwtBlacklistFilter jwtBlacklistFilter,
        com.mycompany.myapp.repository.UserRepository userRepository
    ) {
        this.env = env;
        this.jHipsterProperties = jHipsterProperties;
        this.jwtBlacklistFilter = jwtBlacklistFilter;
        this.userRepository = userRepository;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("auth");
        grantedAuthoritiesConverter.setAuthorityPrefix("");
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOriginPatterns(List.of("http://localhost:9001", "http://localhost:4200"));
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setMaxAge(1800L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        source.registerCorsConfiguration("/management/**", config);
        source.registerCorsConfiguration("/v3/api-docs", config);
        source.registerCorsConfiguration("/swagger-ui/**", config);
        source.registerCorsConfiguration("/websocket/**", config);
        // Allow CORS for actuator endpoints
        source.registerCorsConfiguration("/actuator/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(
        HttpSecurity http,
        MvcRequestMatcher.Builder mvc,
        com.mycompany.myapp.web.rest.AuthenticateController authenticateController
    ) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .addFilterAfter(new SpaWebFilter(), BasicAuthenticationFilter.class)
            .addFilterAfter(jwtBlacklistFilter, BasicAuthenticationFilter.class)
            .headers(headers ->
                headers
                    .contentSecurityPolicy(csp -> csp.policyDirectives(jHipsterProperties.getSecurity().getContentSecurityPolicy()))
                    .frameOptions(FrameOptionsConfig::sameOrigin)
                    .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                    .permissionsPolicyHeader(permissions ->
                        permissions.policy(
                            "camera=(), fullscreen=(self), geolocation=(), gyroscope=(), magnetometer=(), microphone=(), midi=(), payment=(), sync-xhr=()"
                        )
                    )
            )
            .authorizeHttpRequests(authz ->
                authz
                    .requestMatchers(
                        mvc.pattern("/index.html"),
                        mvc.pattern("/*.js"),
                        mvc.pattern("/*.txt"),
                        mvc.pattern("/*.json"),
                        mvc.pattern("/*.map"),
                        mvc.pattern("/*.css")
                    )
                    .permitAll()
                    .requestMatchers(mvc.pattern("/*.ico"), mvc.pattern("/*.png"), mvc.pattern("/*.svg"), mvc.pattern("/*.webapp"))
                    .permitAll()
                    .requestMatchers(mvc.pattern("/app/**"))
                    .permitAll()
                    .requestMatchers(mvc.pattern("/i18n/**"))
                    .permitAll()
                    .requestMatchers(mvc.pattern("/content/**"))
                    .permitAll()
                    .requestMatchers(mvc.pattern("/uploads/**"))
                    .permitAll()
                    .requestMatchers(mvc.pattern("/swagger-ui/**"))
                    .permitAll()
                    .requestMatchers(mvc.pattern(HttpMethod.POST, "/api/authenticate"))
                    .permitAll()
                    .requestMatchers(mvc.pattern(HttpMethod.GET, "/api/authenticate"))
                    .permitAll()
                    .requestMatchers(mvc.pattern("/api/register"))
                    .permitAll()
                    .requestMatchers(mvc.pattern("/api/activate"))
                    .permitAll()
                    .requestMatchers(mvc.pattern("/api/account/reset-password/init"))
                    .permitAll()
                    .requestMatchers(mvc.pattern("/api/account/reset-password/finish"))
                    .permitAll()
                    .requestMatchers(mvc.pattern("/api/admin/**"))
                    .hasAuthority(AuthoritiesConstants.ADMIN)
                    // Public API endpoints - no authentication required
                    .requestMatchers(mvc.pattern(HttpMethod.GET, "/api/products"))
                    .permitAll()
                    .requestMatchers(mvc.pattern(HttpMethod.GET, "/api/products/*"))
                    .permitAll()
                    .requestMatchers(mvc.pattern(HttpMethod.GET, "/api/categories"))
                    .permitAll()
                    .requestMatchers(mvc.pattern(HttpMethod.GET, "/api/categories/*"))
                    .permitAll()
                    .requestMatchers(mvc.pattern(HttpMethod.GET, "/api/reviews"))
                    .permitAll()
                    .requestMatchers(mvc.pattern(HttpMethod.GET, "/api/reviews/*"))
                    .permitAll()
                    .requestMatchers(mvc.pattern("/api/public/**"))
                    .permitAll()
                    // FIX: Allow chat endpoints for guests
                    .requestMatchers(mvc.pattern("/api/chat/conversations"))
                    .permitAll()
                    .requestMatchers(mvc.pattern("/api/support/tickets/**"))
                    .permitAll()
                    .requestMatchers(mvc.pattern("/api/support/tickets"))
                    .permitAll()
                    // ZaloPay payment endpoints
                    .requestMatchers(mvc.pattern("/api/zalopay/callback"))
                    .permitAll()
                    .requestMatchers(mvc.pattern(HttpMethod.POST, "/api/zalopay/create-payment"))
                    .permitAll()
                    .requestMatchers(mvc.pattern(HttpMethod.GET, "/api/zalopay/status/*"))
                    .permitAll()
                    // All other API endpoints require authentication
                    .requestMatchers(mvc.pattern("/api/**"))
                    .authenticated()
                    .requestMatchers(mvc.pattern("/v3/api-docs/**"))
                    .hasAuthority(AuthoritiesConstants.ADMIN)
                    .requestMatchers(mvc.pattern("/management/health"))
                    .permitAll()
                    .requestMatchers(mvc.pattern("/management/health/**"))
                    .permitAll()
                    .requestMatchers(mvc.pattern("/management/info"))
                    .permitAll()
                    .requestMatchers(mvc.pattern("/management/prometheus"))
                    .permitAll()
                    .requestMatchers(mvc.pattern("/management/**"))
                    .hasAuthority(AuthoritiesConstants.ADMIN)
                    // Actuator endpoints
                    .requestMatchers(mvc.pattern("/actuator/health"))
                    .permitAll()
                    .requestMatchers(mvc.pattern("/actuator/health/**"))
                    .permitAll()
                    .requestMatchers(mvc.pattern("/actuator/info"))
                    .permitAll()
                    .requestMatchers(mvc.pattern("/actuator/prometheus"))
                    .permitAll()
                    .requestMatchers(mvc.pattern("/actuator/metrics"))
                    .permitAll()
                    .requestMatchers(mvc.pattern("/actuator/metrics/**"))
                    .permitAll()
                    .requestMatchers(mvc.pattern("/actuator/**"))
                    .hasAuthority(AuthoritiesConstants.ADMIN)
                    // FIX: Allow websocket connection for guests
                    .requestMatchers(mvc.pattern("/websocket/**"))
                    .permitAll()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions ->
                exceptions
                    .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
                    .accessDeniedHandler(new BearerTokenAccessDeniedHandler())
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
            .oauth2Login(oauth2 -> oauth2.successHandler(oauth2AuthenticationSuccessHandler(authenticateController)));

        if (env.acceptsProfiles(Profiles.of(JHipsterConstants.SPRING_PROFILE_DEVELOPMENT))) {
            http.authorizeHttpRequests(authz -> authz.requestMatchers(antMatcher("/h2-console/**")).permitAll());
        }
        return http.build();
    }

    @Bean
    public com.mycompany.myapp.security.oauth2.OAuth2AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler(
        com.mycompany.myapp.web.rest.AuthenticateController authenticateController
    ) {
        return new com.mycompany.myapp.security.oauth2.OAuth2AuthenticationSuccessHandler(authenticateController, userRepository);
    }

    @Bean
    MvcRequestMatcher.Builder mvc(HandlerMappingIntrospector introspector) {
        return new MvcRequestMatcher.Builder(introspector);
    }
}
