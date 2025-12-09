package com.moa.service.auth.impl;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moa.common.exception.BusinessException;
import com.moa.common.exception.ErrorCode;
import com.moa.dao.user.OtpBackupCodeDao;
import com.moa.dao.user.UserDao;
import com.moa.domain.OtpBackupCode;
import com.moa.domain.User;
import com.moa.service.auth.BackupCodeService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class BackupCodeServiceImpl implements BackupCodeService {

	private final OtpBackupCodeDao otpBackupCodeDao;
	private final UserDao userDao;
	private final PasswordEncoder passwordEncoder;
	private final SecureRandom random = new SecureRandom();

	@Override
	public List<String> issueForCurrentUser() {
		String userId = getCurrentUserId();
		User user = userDao.findByUserId(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		if (!Boolean.TRUE.equals(user.getOtpEnabled())) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "OTP가 설정된 계정이 아닙니다.");
		}

		List<OtpBackupCode> exists = otpBackupCodeDao.findValidCodes(userId);
		if (!exists.isEmpty()) {
			throw new BusinessException(ErrorCode.BACKUP_CODE_ALREADY_ISSUED, "이미 발급된 백업 코드가 있습니다.");
		}

		List<String> rawCodes = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			String raw = generateRawCode();
			String hash = passwordEncoder.encode(raw);
			otpBackupCodeDao.insert(userId, hash);
			rawCodes.add(raw);
		}

		return rawCodes;
	}

	@Override
	public void verifyForLogin(String userId, String code) {
		List<OtpBackupCode> codes = otpBackupCodeDao.findValidCodes(userId);
		if (codes == null || codes.isEmpty()) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "유효한 백업 코드가 없습니다.");
		}
		for (OtpBackupCode backup : codes) {
			if (!backup.isUsed() && passwordEncoder.matches(code, backup.getCodeHash())) {
				otpBackupCodeDao.markUsed(backup.getId());
				return;
			}
		}
		throw new BusinessException(ErrorCode.BAD_REQUEST, "잘못된 백업 코드입니다.");
	}

	@Override
	public boolean hasBackupCodesForCurrentUser() {
		String userId = getCurrentUserId();
		List<OtpBackupCode> codes = otpBackupCodeDao.findValidCodes(userId);
		return codes != null && !codes.isEmpty();
	}

	private String getCurrentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || authentication.getName() == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED, "인증 정보가 없습니다.");
		}
		return authentication.getName();
	}

	private String generateRawCode() {
		String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 10; i++) {
			int idx = random.nextInt(chars.length());
			sb.append(chars.charAt(idx));
		}
		return sb.toString();
	}
}
