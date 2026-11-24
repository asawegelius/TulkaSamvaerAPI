package com.tulka.samvaer.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Lets the Angular dev server (another origin) call the API. Only active when
 * {@code samvaer.cors.allowed-origins} is set, which the dev profile does.
 */
@Configuration
@ConditionalOnProperty("samvaer.cors.allowed-origins")
public class DevCorsConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public DevCorsConfig(@Value("${samvaer.cors.allowed-origins}") String[] allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Content-Type", "X-XSRF-TOKEN", "X-Correlation-Id", "X-Stub-Member-Id")
                .exposedHeaders("Location", "X-Correlation-Id")
                .allowCredentials(true);
    }
}
