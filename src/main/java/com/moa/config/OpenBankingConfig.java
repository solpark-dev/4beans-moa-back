package com.moa.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;

@Getter
@Configuration
public class OpenBankingConfig {

    @Value("${openbanking.api.url}")
    private String apiUrl;

    @Value("${openbanking.client-id}")
    private String clientId;

    @Value("${openbanking.client-secret}")
    private String clientSecret;

    @Value("${openbanking.callback-url}")
    private String callbackUrl;

    @Value("${openbanking.institution-code}")
    private String institutionCode;

    @Value("${openbanking.platform.bank-code}")
    private String platformBankCode;

    @Value("${openbanking.platform.account-number}")
    private String platformAccountNumber;

    @Value("${openbanking.platform.account-holder}")
    private String platformAccountHolder;

    @Value("${openbanking.platform.client-num}")
    private String platformClientNum;
}
