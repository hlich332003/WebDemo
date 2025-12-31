package com.mycompany.myapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration for serving static resources.
 */
@Configuration
public class StaticResourceConfiguration implements WebMvcConfigurer {

    @Value("${application.upload-dir:uploads/products}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve uploaded images from /uploads/** path
        registry.addResourceHandler("/uploads/**").addResourceLocations("file:" + uploadDir + "/").setCachePeriod(3600); // Cache for 1 hour
    }
}
