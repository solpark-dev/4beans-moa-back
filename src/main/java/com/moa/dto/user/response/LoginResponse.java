package com.moa.dto.user.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class LoginResponse {

	private boolean otpRequired;

	private String otpToken;
	private String accessToken;
	private String refreshToken;
	private Long accessTokenExpiresIn;
	private String userId;
}
