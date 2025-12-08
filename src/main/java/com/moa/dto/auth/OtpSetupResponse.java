package com.moa.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpSetupResponse {

	private String secret;
	private String otpAuthUrl;
	private boolean enabled;
}
