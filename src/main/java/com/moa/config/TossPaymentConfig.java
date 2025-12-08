package com.moa.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import lombok.Getter;

@Configuration
@Getter
public class TossPaymentConfig {

    @Value("${toss.client.api-key:test_ck_dummy}")
    private String clientApiKey;

    @Value("${toss.secret.api-key:test_sk_dummy}")
    private String secretApiKey;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
