package com.moa.service.auth;

import com.moa.dto.auth.OtpLoginVerifyRequest;
import com.moa.dto.auth.TokenResponse;
import com.moa.dto.user.request.LoginRequest;
import com.moa.dto.user.response.LoginResponse;

public interface AuthService {

	LoginResponse login(LoginRequest request);

	TokenResponse refresh(String refreshToken);

	void logout(String accessToken, String refreshToken);

	void verifyEmail(String token);

	TokenResponse verifyLoginOtp(OtpLoginVerifyRequest request);
}
