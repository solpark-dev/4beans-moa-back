package com.moa.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OtpLoginVerifyRequest {

	@NotBlank
	private String otpToken;

	@NotBlank
	private String code;
}
