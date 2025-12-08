package com.moa.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;

@Configuration
public class TotpConfig {

	@Bean
	public SecretGenerator secretGenerator() {
		return new DefaultSecretGenerator();
	}

	@Bean
	public TimeProvider timeProvider() {
		return new SystemTimeProvider();
	}

	@Bean
	public CodeGenerator codeGenerator() {
		return new DefaultCodeGenerator();
	}

	@Bean
	public CodeVerifier codeVerifier(CodeGenerator codeGenerator, TimeProvider timeProvider) {
		DefaultCodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
		verifier.setTimePeriod(30);
		verifier.setAllowedTimePeriodDiscrepancy(1);
		return verifier;
	}
}
