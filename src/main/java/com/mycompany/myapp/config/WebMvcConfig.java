package com.mycompany.myapp.config;

import com.mycompany.myapp.web.rest.interceptor.RequestLoggingInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final RequestLoggingInterceptor requestLoggingInterceptor;

    @Autowired
    public WebMvcConfig(RequestLoggingInterceptor requestLoggingInterceptor) {
        this.requestLoggingInterceptor = requestLoggingInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry
            .addInterceptor(requestLoggingInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns(
                "/api/public/**",
                "/api/authenticate",
                "/api/register",
                "/api/activate",
                "/api/account/reset-password/init",
                "/api/account/reset-password/finish"
            );
    }
}
