package com.moa.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Getter
@Slf4j
public class TossPaymentConfig implements InitializingBean {

	@Value("${toss.client.api-key:test_ck_dummy}")
	private String clientApiKey;

	@Value("${toss.secret.api-key:test_sk_dummy}")
	private String secretApiKey;

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		String maskedKey = (secretApiKey != null && secretApiKey.length() > 5) ? secretApiKey.substring(0, 5) + "***"
				: "NULL_OR_SHORT";
		log.error("[CONFIG CHECK] Loaded Secret Key Prefix: {}", maskedKey);
	}
}
