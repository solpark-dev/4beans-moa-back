package com.moa.web.auth;

import java.util.Map;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moa.common.exception.ApiResponse;
import com.moa.common.exception.BusinessException;
import com.moa.common.exception.ErrorCode;
import com.moa.dto.auth.OtpLoginVerifyRequest;
import com.moa.dto.auth.OtpSetupResponse;
import com.moa.dto.auth.OtpVerifyRequest;
import com.moa.dto.auth.TokenResponse;
import com.moa.dto.auth.UnlockAccountRequest;
import com.moa.dto.user.request.LoginRequest;
import com.moa.dto.user.response.LoginResponse;
import com.moa.service.auth.AuthService;
import com.moa.service.auth.OtpService;
import com.moa.service.passauth.PassAuthService;
import com.moa.service.user.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthRestController {

	private final AuthService authService;
	private final OtpService otpService;
	private final PassAuthService passAuthService;
	private final UserService userService;

	@PostMapping("/login")
	public ApiResponse<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
		return ApiResponse.success(authService.login(request));
	}

	@PostMapping("/login/otp-verify")
	public ApiResponse<TokenResponse> verifyLoginOtp(@RequestBody @Valid OtpLoginVerifyRequest request) {
		return ApiResponse.success(authService.verifyLoginOtp(request));
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

	@PostMapping("/otp/setup")
	public ApiResponse<OtpSetupResponse> setupOtp() {
		return ApiResponse.success(otpService.setup());
	}

	@PostMapping("/otp/verify")
	public ApiResponse<Void> verifyOtp(@RequestBody @Valid OtpVerifyRequest request) {
		otpService.verify(request);
		return ApiResponse.success(null);
	}

	@PostMapping("/otp/disable")
	public ApiResponse<Void> disableOtp() {
		otpService.disable();
		return ApiResponse.success(null);
	}

	@PostMapping("/otp/disable-verify")
	public ApiResponse<?> disableVerify(@RequestBody OtpVerifyRequest request) {
		String userId = SecurityContextHolder.getContext().getAuthentication().getName();

		otpService.disableWithCode(userId, request.getCode());
		return ApiResponse.success(null);
	}

	@PostMapping("/unlock")
	public ApiResponse<Void> unlockAccount(@RequestBody @Valid UnlockAccountRequest request) {
		Map<String, Object> data;
		try {
			data = passAuthService.verifyCertification(request.getImpUid());
		} catch (Exception e) {
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "본인인증 처리 중 오류가 발생했습니다.");
		}

		Object phoneObj = data.get("phone");
		String phone = phoneObj != null ? phoneObj.toString() : null;
		Object ciObj = data.get("ci");
		String ci = ciObj != null ? ciObj.toString() : null;

		if (phone == null || phone.isBlank()) {
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "본인인증 결과에 휴대폰 번호가 없습니다.");
		}

		userService.unlockByCertification(request.getUserId(), phone, ci);

		return ApiResponse.success(null);
	}
}
