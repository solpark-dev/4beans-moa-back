package com.moa.service.auth.impl;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moa.common.exception.BusinessException;
import com.moa.common.exception.ErrorCode;
import com.moa.dao.user.UserDao;
import com.moa.dto.auth.OtpSetupResponse;
import com.moa.dto.auth.OtpVerifyRequest;
import com.moa.service.auth.OtpService;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.secret.SecretGenerator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

	private final UserDao userDao;
	private final SecretGenerator secretGenerator;
	private final CodeVerifier codeVerifier;

	@Value("${app.otp.issuer:MOA}")
	private String issuer;

	@Override
	@Transactional
	public OtpSetupResponse setup() {
		String userId = getCurrentUserId();
		String secret = secretGenerator.generate();
		userDao.updateOtpSettings(userId, secret, false);
		String otpAuthUrl = buildOtpAuthUrl(userId, secret);
		Boolean enabled = userDao.isOtpEnabled(userId);
		boolean alreadyEnabled = enabled != null && enabled;
		return OtpSetupResponse.builder().secret(secret).otpAuthUrl(otpAuthUrl).enabled(alreadyEnabled).build();
	}

	@Override
	@Transactional
	public void verify(OtpVerifyRequest request) {
		String userId = getCurrentUserId();
		String secret = userDao.findOtpSecret(userId);
		if (secret == null) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "OTP가 설정되지 않았습니다.");
		}
		boolean valid = codeVerifier.isValidCode(secret, request.getCode());
		if (!valid) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "잘못된 OTP 코드입니다.");
		}
		userDao.updateOtpSettings(userId, secret, true);
	}

	@Override
	@Transactional
	public void disable() {
		String userId = getCurrentUserId();
		userDao.updateOtpSettings(userId, null, false);
	}

	@Override
	public void verifyLoginCode(String userId, String code) {
		String secret = userDao.findOtpSecret(userId);
		if (secret == null) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "OTP가 설정되지 않았습니다.");
		}
		boolean valid = codeVerifier.isValidCode(secret, code);
		if (!valid) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "잘못된 OTP 코드입니다.");
		}
	}

	private String getCurrentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || authentication.getName() == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED, "인증 정보가 없습니다.");
		}
		return authentication.getName();
	}

	private String buildOtpAuthUrl(String userId, String secret) {
		String encodedIssuer = URLEncoder.encode(issuer, StandardCharsets.UTF_8);
		String encodedUserId = URLEncoder.encode(userId, StandardCharsets.UTF_8);
		return "otpauth://totp/" + encodedIssuer + ":" + encodedUserId + "?secret=" + secret + "&issuer="
				+ encodedIssuer + "&digits=6&period=30";
	}

	@Override
	@Transactional
	public void disableWithCode(String userId, String code) {

		String secret = userDao.findOtpSecret(userId);
		if (secret == null) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "OTP가 설정되어 있지 않습니다.");
		}

		boolean valid = codeVerifier.isValidCode(secret, code);
		if (!valid) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "잘못된 OTP 코드입니다.");
		}

		userDao.updateOtpSettings(userId, null, false);
	}
}
