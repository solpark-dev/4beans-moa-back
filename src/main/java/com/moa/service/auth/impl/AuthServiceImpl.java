package com.moa.service.auth.impl;

import java.time.LocalDateTime;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moa.auth.provider.JwtProvider;
import com.moa.common.exception.BusinessException;
import com.moa.common.exception.ErrorCode;
import com.moa.dao.user.EmailVerificationDao;
import com.moa.dao.user.UserDao;
import com.moa.domain.EmailVerification;
import com.moa.domain.User;
import com.moa.domain.enums.UserStatus;
import com.moa.dto.auth.TokenResponse;
import com.moa.dto.user.request.LoginRequest;
import com.moa.service.auth.AuthService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

	private final UserDao userDao;
	private final EmailVerificationDao emailVerificationDao;
	private final PasswordEncoder passwordEncoder;
	private final JwtProvider jwtProvider;

	@Override
	public TokenResponse login(LoginRequest request) {
		User user = userDao.findByUserIdIncludeDeleted(request.getUserId().toLowerCase())
				.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_LOGIN));

		if (user.getDeleteDate() != null && user.getStatus() == UserStatus.WITHDRAW) {
			throw new BusinessException(ErrorCode.ACCOUNT_WITHDRAW, "탈퇴한 계정입니다.");
		}

		if (user.getStatus() == UserStatus.BLOCK) {
			if (user.getUnlockScheduledAt() != null && LocalDateTime.now().isAfter(user.getUnlockScheduledAt())) {
				userDao.updateUserStatus(user.getUserId(), UserStatus.ACTIVE);
				userDao.resetLoginFailCount(user.getUserId());
				user.setStatus(UserStatus.ACTIVE);
			} else {
				throw new BusinessException(ErrorCode.FORBIDDEN, "잠금 처리된 계정입니다.");
			}
		}

		if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
			userDao.increaseLoginFailCount(user.getUserId());
			int failCount = userDao.getFailCount(user.getUserId());
			if (failCount >= 5) {
				userDao.blockUser(user.getUserId(), LocalDateTime.now().plusMinutes(30));
				throw new BusinessException(ErrorCode.FORBIDDEN, "로그인 5회 실패로 잠금 처리되었습니다.");
			}
			throw new BusinessException(ErrorCode.INVALID_LOGIN);
		}

		if (user.getStatus() == UserStatus.PENDING) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "이메일 인증이 필요합니다.");
		}

		userDao.resetLoginFailCount(user.getUserId());
		userDao.updateLastLoginDate(user.getUserId());

		Authentication authentication = new UsernamePasswordAuthenticationToken(user.getUserId(), null,
				java.util.List.of(new SimpleGrantedAuthority(user.getRole())));

		return jwtProvider.generateToken(authentication);
	}

	@Override
	public TokenResponse refresh(String refreshToken) {
		try {
			return jwtProvider.refresh(refreshToken);
		} catch (Exception e) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED, "리프레시 토큰이 유효하지 않습니다.");
		}
	}

	@Override
	public void logout(String accessToken, String refreshToken) {
	}

	@Override
	@Transactional
	public void verifyEmail(String token) {
		EmailVerification verification = emailVerificationDao.findByToken(token)
				.orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "유효하지 않은 토큰입니다."));

		System.out.println("verifyEmail token=" + token + ", expiresAt=" + verification.getExpiresAt());

		if (verification.getExpiresAt() == null || verification.getExpiresAt().isBefore(LocalDateTime.now())) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "만료된 인증 링크입니다.");
		}

		if (verification.getVerifiedAt() != null) {
			return;
		}

		emailVerificationDao.updateVerifiedAt(token);
		userDao.updateUserStatus(verification.getUserId(), UserStatus.ACTIVE);
	}
}