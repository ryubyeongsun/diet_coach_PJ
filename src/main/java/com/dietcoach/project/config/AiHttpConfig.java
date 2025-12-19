package com.dietcoach.project.config;

import java.time.Duration;
import java.net.http.HttpClient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class AiHttpConfig {

    @Bean
    public RestClient gmsOpenAiRestClient(
            RestClient.Builder builder,
            @Value("${gms.openai.base-url}") String baseUrl,
            @Value("${gms.openai.timeout-ms:8000}") long timeoutMs
    ) {
        Duration timeout = Duration.ofMillis(timeoutMs);

        // ✅ JDK HttpClient: connect timeout
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();

        // ✅ Spring request factory: read timeout
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(timeout);

        return builder
                .baseUrl(baseUrl)      // ex) https://gms.ssafy.io/gmsapi/api.openai.com/v1
                .requestFactory(factory)
                .build();
    }
}
