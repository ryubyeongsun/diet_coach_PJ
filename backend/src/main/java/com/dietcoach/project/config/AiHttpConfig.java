package com.dietcoach.project.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class AiHttpConfig {

        @Bean
        public RestClient gmsOpenAiRestClient(
                        @Value("${gms.openai.base-url:}") String baseUrl,
                        @Value("${gms.openai.timeout-ms:8000}") long timeoutMs) {
                try {
                        log.info("Creating gmsOpenAiRestClient with baseUrl: {}, timeout: {}ms", baseUrl, timeoutMs);

                        // SimpleClientHttpRequestFactory 사용 (가장 기본적이고 호환성 높은 팩토리)
                        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                        factory.setConnectTimeout((int) timeoutMs);
                        factory.setReadTimeout((int) timeoutMs);

                        return RestClient.builder()
                                        .baseUrl(baseUrl)
                                        .requestFactory(factory)
                                        .build();

                } catch (Exception e) {
                        log.error("Failed to create gmsOpenAiRestClient bean: {}", e.getMessage(), e);
                        throw new IllegalStateException("Failed to create gmsOpenAiRestClient bean", e);
                }
        }
}
