package com.moa.web.auth;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moa.common.exception.ApiResponse;
import com.moa.common.exception.BusinessException;
import com.moa.common.exception.ErrorCode;
import com.moa.dto.auth.BackupCodeIssueResponse;
import com.moa.dto.auth.BackupCodeLoginRequest;
import com.moa.dto.auth.OtpLoginVerifyRequest;
import com.moa.dto.auth.OtpSetupResponse;
import com.moa.dto.auth.OtpVerifyRequest;
import com.moa.dto.auth.TokenResponse;
import com.moa.dto.auth.UnlockAccountRequest;
import com.moa.dto.user.request.LoginRequest;
import com.moa.dto.user.response.LoginResponse;
import com.moa.service.auth.AuthService;
import com.moa.service.auth.BackupCodeService;
import com.moa.service.auth.LoginHistoryService;
import com.moa.service.auth.OtpService;
import com.moa.service.passauth.PassAuthService;
import com.moa.service.user.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthRestController {

	@Value("${app.frontend-url}")
	private String frontendUrl;
	private final AuthService authService;
	private final OtpService otpService;
	private final PassAuthService passAuthService;
	private final UserService userService;
	private final LoginHistoryService loginHistoryService;
	private final BackupCodeService backupCodeService;

	@PostMapping("/login")
	public ApiResponse<LoginResponse> login(@RequestBody @Valid LoginRequest request, HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) {

		String clientIp = extractClientIp(httpRequest);
		String userAgent = httpRequest.getHeader("User-Agent");
		String loginType = "PASSWORD";

		try {
			LoginResponse response = authService.login(request);

			String userId = extractUserIdFromLoginRequest(request);
			if (userId == null || userId.isBlank()) {
				userId = response.getUserId();
			}

			loginHistoryService.recordSuccess(userId, loginType, clientIp, userAgent);

			if (!response.isOtpRequired()) {
				ResponseCookie accessCookie = ResponseCookie.from("ACCESS_TOKEN", response.getAccessToken())
						.httpOnly(true).secure(true).sameSite("None").path("/")
						.maxAge(response.getAccessTokenExpiresIn()).build();

				ResponseCookie refreshCookie = ResponseCookie.from("REFRESH_TOKEN", response.getRefreshToken())
						.httpOnly(true).secure(true).sameSite("None").path("/").maxAge(60 * 60 * 24 * 14).build();

				httpResponse.addHeader("Set-Cookie", accessCookie.toString());
				httpResponse.addHeader("Set-Cookie", refreshCookie.toString());
			}

			return ApiResponse.success(response);

		} catch (BusinessException e) {
			throw e;
		}
	}

	@PostMapping("/login/otp-verify")
	public ApiResponse<TokenResponse> verifyLoginOtp(@RequestBody @Valid OtpLoginVerifyRequest request,
			HttpServletRequest httpRequest) {

		String clientIp = extractClientIp(httpRequest);
		String userAgent = httpRequest.getHeader("User-Agent");
		String loginType = "OTP";
		String userId = request.getUserId();
		if (userId == null || userId.isBlank()) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "userId가 필요합니다.");
		}

		TokenResponse tokenResponse = authService.verifyLoginOtp(request);
		loginHistoryService.recordSuccess(userId, loginType, clientIp, userAgent);

		return ApiResponse.success(tokenResponse);
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

	@GetMapping("/verify-email")
	public ResponseEntity<Void> verifyEmailByLink(@RequestParam("token") String token) {
		try {
			authService.verifyEmail(token);
			String location = frontendUrl + "/email-verified?result=success";
			return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, location).build();
		} catch (Exception e) {
			String msg = URLEncoder.encode("인증에 실패했습니다.", StandardCharsets.UTF_8);
			String location = frontendUrl + "/email-verified?result=fail&message=" + msg;
			return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, location).build();
		}
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

	@PostMapping("/otp/backup/issue")
	public ApiResponse<BackupCodeIssueResponse> issueBackupCodes() {
		List<String> codes = backupCodeService.issueForCurrentUser();
		BackupCodeIssueResponse response = BackupCodeIssueResponse.builder().codes(codes).issued(true).build();
		return ApiResponse.success(response);
	}

	@PostMapping("/login/backup-verify")
	public ApiResponse<TokenResponse> verifyLoginBackup(@RequestBody @Valid BackupCodeLoginRequest request,
			HttpServletRequest httpRequest) {
		String clientIp = extractClientIp(httpRequest);
		String userAgent = httpRequest.getHeader("User-Agent");
		String loginType = "OTP_BACKUP";

		TokenResponse tokenResponse = authService.verifyLoginBackupCode(request);

		String userId = request.getUserId();
		if (userId == null || userId.isBlank()) {
			userId = SecurityContextHolder.getContext().getAuthentication() != null
					? SecurityContextHolder.getContext().getAuthentication().getName()
					: null;
		}

		if (userId != null && !userId.isBlank()) {
			loginHistoryService.recordSuccess(userId, loginType, clientIp, userAgent);
		}

		return ApiResponse.success(tokenResponse);
	}

	@GetMapping("/otp/backup/list")
	public ApiResponse<BackupCodeIssueResponse> getBackupCodeList() {
		boolean issued = backupCodeService.hasBackupCodesForCurrentUser();
		BackupCodeIssueResponse response = BackupCodeIssueResponse.builder().codes(List.of()).issued(issued).build();
		return ApiResponse.success(response);
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

	@PostMapping("/exists-by-email")
	public ApiResponse<Void> existsByEmail(@RequestBody Map<String, String> body) {

		String email = body.get("email");

		if (email == null || email.isBlank()) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "이메일을 입력해주세요.");
		}

		try {
			userService.unlockByCertification(email, "__CHECK_ONLY__",
					null);
		} catch (BusinessException e) {

			if (e.getErrorCode() == ErrorCode.USER_NOT_FOUND) {
				throw e;
			}

		}

		return ApiResponse.success(null);
	}

	private String extractClientIp(HttpServletRequest request) {
		String ip = request.getHeader("X-Forwarded-For");
		if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
			int commaIndex = ip.indexOf(',');
			if (commaIndex > 0) {
				ip = ip.substring(0, commaIndex);
			}
			return ip.trim();
		}

		ip = request.getHeader("X-Real-IP");
		if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
			return ip.trim();
		}

		ip = request.getRemoteAddr();
		if ("0:0:0:0:0:0:0:1".equals(ip)) {
			return "127.0.0.1";
		}

		return ip;
	}

	private String extractUserIdFromLoginRequest(LoginRequest request) {
		try {
			return request.getUserId();
		} catch (Exception e) {
			return null;
		}
	}

	@PostMapping("/restore")
	public ApiResponse<Map<String, Object>> restore(@RequestBody @Valid com.moa.dto.auth.RestoreAccountRequest request,
			HttpServletRequest httpRequest, HttpServletResponse httpResponse) {

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

		String userId = request.getUserId().toLowerCase();

		userService.restoreByCertification(userId, phone, ci);

		TokenResponse token = authService.issueToken(userId);

		boolean isHttps = "https".equalsIgnoreCase(httpRequest.getHeader("X-Forwarded-Proto"))
				|| httpRequest.isSecure();

		ResponseCookie accessCookie = ResponseCookie.from("ACCESS_TOKEN", token.getAccessToken()).httpOnly(true)
				.secure(isHttps).sameSite(isHttps ? "None" : "Lax").path("/").maxAge(token.getAccessTokenExpiresIn())
				.build();

		ResponseCookie refreshCookie = ResponseCookie.from("REFRESH_TOKEN", token.getRefreshToken()).httpOnly(true)
				.secure(isHttps).sameSite(isHttps ? "None" : "Lax").path("/").maxAge(60 * 60 * 24 * 14).build();

		httpResponse.addHeader("Set-Cookie", accessCookie.toString());
		httpResponse.addHeader("Set-Cookie", refreshCookie.toString());

		String clientIp = extractClientIp(httpRequest);
		String userAgent = httpRequest.getHeader("User-Agent");
		loginHistoryService.recordSuccess(userId, "RESTORE", clientIp, userAgent);

		return ApiResponse.success(Map.of("restored", true, "userId", userId, "accessToken", token.getAccessToken(),
				"refreshToken", token.getRefreshToken(), "accessTokenExpiresIn", token.getAccessTokenExpiresIn()));
	}

}
