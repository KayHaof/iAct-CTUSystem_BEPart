package com.example.activityservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    @Primary
    public RestTemplate restTemplate() {
        return createRestTemplate(5000, 5000);
    }

    @Bean("aiRestTemplate")
    public RestTemplate aiRestTemplate(
            @Value("${app.services.ai-connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${app.services.ai-read-timeout-ms:120000}") int readTimeoutMs) {
        return createRestTemplate(connectTimeoutMs, readTimeoutMs);
    }

    private RestTemplate createRestTemplate(int connectTimeoutMs, int readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        return new RestTemplate(factory);
    }
}
