// com.moa.web.auth.AuthRestController.java
package com.moa.web.auth;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moa.common.exception.ApiResponse;
import com.moa.dto.auth.TokenResponse;
import com.moa.dto.user.request.LoginRequest;
import com.moa.service.auth.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthRestController {

	private final AuthService authService;

	@PostMapping("/login")
	public ApiResponse<TokenResponse> login(@RequestBody @Valid LoginRequest request) {
		return ApiResponse.success(authService.login(request));
	}

	@PostMapping("/refresh")
	public ApiResponse<TokenResponse> refresh(@RequestHeader("Refresh-Token") String refreshToken) {
		return ApiResponse.success(authService.refresh(refreshToken));
	}

	@PostMapping("/logout")
	public ApiResponse<Void> logout(@RequestHeader(value = "Authorization", required = false) String accessToken,
			@RequestHeader(value = "Refresh-Token", required = false) String refreshToken) {

		authService.logout(accessToken, refreshToken);
		return ApiResponse.success(null);
	}

	@PostMapping("/verify-email")
	public ApiResponse<Void> verifyEmail(@RequestParam("token") String token) {
		authService.verifyEmail(token);
		return ApiResponse.success(null);
	}
}
