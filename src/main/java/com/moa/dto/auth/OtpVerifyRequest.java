package com.moa.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OtpVerifyRequest {

	@NotBlank
	private String code;
}
