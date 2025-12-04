package com.dietcoach.project.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Common HTTP client configuration for external API calls.
 */
@Configuration
public class HttpClientConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
