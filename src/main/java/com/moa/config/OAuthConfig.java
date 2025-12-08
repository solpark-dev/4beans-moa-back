package com.moa.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({ KakaoOAuthProperties.class, GoogleOAuthProperties.class })
public class OAuthConfig {
}
