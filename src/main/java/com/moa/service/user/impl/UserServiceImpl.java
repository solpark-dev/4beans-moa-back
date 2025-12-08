package com.moa.service.user.impl;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.moa.common.exception.BusinessException;
import com.moa.common.exception.ErrorCode;
import com.moa.dao.admin.AdminDao;
import com.moa.dao.oauth.OAuthAccountDao;
import com.moa.dao.user.EmailVerificationDao;
import com.moa.dao.user.UserDao;
import com.moa.domain.EmailVerification;
import com.moa.domain.OAuthAccount;
import com.moa.domain.User;
import com.moa.domain.enums.UserStatus;
import com.moa.dto.community.response.PageResponse;
import com.moa.dto.user.request.AdminUserSearchRequest;
import com.moa.dto.user.request.CommonCheckRequest;
import com.moa.dto.user.request.DeleteUserRequest;
import com.moa.dto.user.request.PasswordResetRequest;
import com.moa.dto.user.request.PasswordResetStartRequest;
import com.moa.dto.user.request.PasswordUpdateRequest;
import com.moa.dto.user.request.UserCreateRequest;
import com.moa.dto.user.request.UserUpdateRequest;
import com.moa.dto.user.response.AdminUserListItemResponse;
import com.moa.dto.user.response.CommonCheckResponse;
import com.moa.dto.user.response.PasswordResetTokenResponse;
import com.moa.dto.user.response.UserResponse;
import com.moa.service.mail.EmailService;
import com.moa.service.oauth.OAuthAccountService;
import com.moa.service.user.UserAddValidator;
import com.moa.service.user.UserService;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

	private final UserDao userDao;
	private final OAuthAccountDao oauthAccountDao;
	private final EmailVerificationDao emailVerificationDao;
	private final EmailService emailService;
	private final PasswordEncoder passwordEncoder;
	private final OAuthAccountService oauthAccountService;
	private final AdminDao adminDao;
	private final UserAddValidator userAddValidator;

	@Value("${app.upload.user.profile-dir}")
	private String profileUploadDir;

	@Value("${app.upload.user.profile-url-prefix}")
	private String profileUrlPrefix;

	private static final Pattern PASSWORD_PATTERN = Pattern
			.compile("^(?=.*[A-Za-z])(?:(?=.*[0-9])|(?=.*[^A-Za-z0-9])).{8,20}$");

	@Override
	public void validatePasswordRule(String password) {
		if (!PASSWORD_PATTERN.matcher(password).matches()) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "비밀번호 형식이 올바르지 않습니다.");
		}
	}

	@Override
	public void validatePasswordConfirm(String password, String passwordConfirm) {
		if (!password.equals(passwordConfirm)) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "비밀번호가 일치하지 않습니다.");
		}
	}

	@Override
	public void checkCurrentPassword(String userId, String currentPassword) {
		User user = userDao.findByUserId(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		ensureNotBlocked(user);

		if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED, "현재 비밀번호가 올바르지 않습니다.");
		}
	}

	@Override
	public CommonCheckResponse check(CommonCheckRequest request) {
		boolean available;

		if ("email".equals(request.getType())) {
			available = userDao.existsByUserId(request.getValue()) == 0;
		} else if ("nickname".equals(request.getType())) {
			available = userDao.existsByNickname(request.getValue()) == 0;
		} else if ("phone".equals(request.getType())) {
			available = userDao.existsByPhone(request.getValue()) == 0;
		} else {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "잘못된 type 값입니다.");
		}

		return CommonCheckResponse.builder().available(available).build();
	}

	@Override
	public void updatePassword(String userId, PasswordUpdateRequest request) {
		validatePasswordRule(request.getNewPassword());
		validatePasswordConfirm(request.getNewPassword(), request.getNewPasswordConfirm());

		User user = userDao.findByUserId(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		ensureNotBlocked(user);

		userDao.updatePassword(userId, passwordEncoder.encode(request.getNewPassword()));
		userDao.resetLoginFailCount(userId);
	}

	@Override
	@Transactional
	public PasswordResetTokenResponse startPasswordReset(PasswordResetStartRequest request) {
		User user = userDao.findByUserId(request.getUserId())
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));

		String token = UUID.randomUUID().toString();
		userDao.savePasswordResetToken(user.getUserId(), token);
		return PasswordResetTokenResponse.builder().token(token).build();
	}

	@Override
	@Transactional
	public void resetPassword(PasswordResetRequest request) {
		if (!request.getPassword().equals(request.getPasswordConfirm())) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "비밀번호 확인이 일치하지 않습니다.");
		}

		User user = userDao.findByResetToken(request.getToken())
				.orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR, "유효하지 않은 토큰입니다."));

		String encoded = passwordEncoder.encode(request.getPassword());

		userDao.updatePassword(user.getUserId(), encoded);

		userDao.clearPasswordResetToken(user.getUserId());
	}

	@Override
	@Transactional(noRollbackFor = BusinessException.class)
	public UserResponse addUser(UserCreateRequest request) {

	    boolean isSocial = request.getProvider() != null && !request.getProvider().isBlank()
	            && request.getProviderUserId() != null && !request.getProviderUserId().isBlank();

	    userAddValidator.validateForSignup(request);

	    if (userDao.existsByPhone(request.getPhone()) > 0) {
	        if (!isSocial) {
	            throw new BusinessException(ErrorCode.DUPLICATED_PHONE, "이미 사용중인 휴대폰번호입니다.");
	        }
	        String provider = request.getProvider();
	        String providerUserId = request.getProviderUserId();
	        String phone = request.getPhone();
	        Optional<String> existingUserIdOpt = userDao.findUserIdByPhone(phone);
	        if (existingUserIdOpt.isEmpty()) {
	            throw new BusinessException(ErrorCode.CONFLICT, "휴대폰번호로 회원을 찾을 수 없습니다.");
	        }
	        String existingUserId = existingUserIdOpt.get();
	        OAuthAccount existing = oauthAccountService.getOAuthByProvider(provider, providerUserId);
	        if (existing != null && !existing.getUserId().equals(existingUserId)) {
	            throw new BusinessException(ErrorCode.CONFLICT, "이미 다른 계정과 연결된 소셜 계정입니다.");
	        }
	        oauthAccountService.connectOAuthAccount(existingUserId, provider, providerUserId);
	        User existingUser = userDao.findByUserId(existingUserId)
	                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
	        return UserResponse.from(existingUser);
	    }

	    String profileImageUrl = null;
	    if (request.getProfileImageBase64() != null && !request.getProfileImageBase64().isBlank()) {
	        profileImageUrl = saveProfileImageFromBase64(request.getProfileImageBase64());
	    }

	    LocalDateTime now = LocalDateTime.now();
	    LocalDateTime passCertifiedAt = null;
	    if (request.getCi() != null && !request.getCi().isBlank()) {
	        passCertifiedAt = now;
	    }

	    User user = User.builder()
	            .userId(request.getUserId().toLowerCase())
	            .password(isSocial ? null : passwordEncoder.encode(request.getPassword()))
	            .nickname(request.getNickname())
	            .phone(request.getPhone())
	            .profileImage(profileImageUrl)
	            .role("USER")
	            .status(isSocial ? UserStatus.ACTIVE : UserStatus.PENDING)
	            .regDate(now)
	            .ci(request.getCi())
	            .passCertifiedAt(passCertifiedAt)
	            .loginFailCount(0)
	            .build();

	    userDao.insertUser(user);

	    if (!isSocial) {
	        emailVerificationDao.expirePreviousTokens(user.getUserId());

	        String token = UUID.randomUUID().toString();

	        EmailVerification emailVerification = EmailVerification.builder()
	                .userId(user.getUserId())
	                .token(token)
	                .expiresAt(LocalDateTime.now().plusHours(24))
	                .build();

	        emailVerificationDao.insert(emailVerification);

	        emailService.sendSignupVerificationEmail(user.getUserId(), user.getNickname(), token);
	    }

	    if (isSocial) {
	        OAuthAccount account = OAuthAccount.builder()
	                .oauthId(UUID.randomUUID().toString())
	                .provider(request.getProvider())
	                .providerUserId(request.getProviderUserId())
	                .userId(user.getUserId())
	                .build();
	        oauthAccountService.addOAuthAccount(account);
	    }

	    return UserResponse.from(user);
	}


	@Override
	@Transactional(readOnly = true)
	public PageResponse<AdminUserListItemResponse> getAdminUserList(AdminUserSearchRequest request) {
		int page = request.getPageOrDefault();
		int size = request.getSizeOrDefault();
		int offset = (page - 1) * size;

		request.setSize(size);
		request.setOffset(offset);

		long totalCount = adminDao.countAdminUsers(request);
		List<AdminUserListItemResponse> content = totalCount > 0 ? adminDao.findAdminUsers(request)
				: Collections.emptyList();

		return new PageResponse<>(content, page, size, totalCount);
	}

	@Override
	@Transactional(readOnly = true)
	public UserResponse getUserDetailForAdmin(String userId) {
		User user = userDao.findByUserIdIncludeDeleted(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		List<OAuthAccount> accounts = oauthAccountDao.getOAuthAccountList(userId);

		UserResponse response = UserResponse.from(user, accounts);

		if (adminDao.findActiveBlacklistByUserId(userId) != null) {
			response.setBlacklisted(true);
		}

		return response;
	}

	@Override
	@Transactional(readOnly = true)
	public UserResponse getCurrentUser() {

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null || !authentication.isAuthenticated()
				|| "anonymousUser".equals(authentication.getPrincipal())) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED, "로그인 정보가 없습니다.");
		}

		String userId = authentication.getName();

		User user = userDao.findByUserId(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));

		List<OAuthAccount> oauthAccounts = oauthAccountDao.getOAuthAccountList(userId);

		return UserResponse.from(user, oauthAccounts);
	}

	@Override
	public String findUserIdByPhone(String phone) {
		return userDao.findUserIdByPhone(phone)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "해당 휴대폰 번호로 가입된 계정이 없습니다."));
	}

	@Override
	public User findUserIncludeDeleted(String userId) {
		return userDao.findByUserIdIncludeDeleted(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));
	}

	@Override
	public UserResponse updateUser(String userId, UserUpdateRequest request) {
		User user = userDao.findByUserId(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		ensureNotBlocked(user);

		if (!user.getNickname().equals(request.getNickname()) && userDao.existsByNickname(request.getNickname()) > 0) {
			throw new BusinessException(ErrorCode.CONFLICT, "이미 사용 중인 닉네임입니다.");
		}

		User updated = User.builder().userId(user.getUserId()).password(user.getPassword())
				.nickname(request.getNickname()).phone(request.getPhone()).profileImage(request.getProfileImage())
				.agreeMarketing(request.isAgreeMarketing()).role(user.getRole()).status(user.getStatus())
				.regDate(user.getRegDate()).ci(user.getCi()).passCertifiedAt(user.getPassCertifiedAt())
				.lastLoginDate(user.getLastLoginDate()).loginFailCount(user.getLoginFailCount())
				.unlockScheduledAt(user.getUnlockScheduledAt()).deleteDate(user.getDeleteDate())
				.deleteType(user.getDeleteType()).deleteDetail(user.getDeleteDetail()).build();

		userDao.updateUserProfile(updated);

		return UserResponse.from(updated);
	}

	@Override
	public String uploadProfileImage(String userId, MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "업로드할 파일이 없습니다.");
		}

		User user = userDao.findByUserId(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		ensureNotBlocked(user);

		try {
			java.io.File dir = new java.io.File(profileUploadDir);
			if (!dir.exists()) {
				dir.mkdirs();
			}

			String ext = org.springframework.util.StringUtils.getFilenameExtension(file.getOriginalFilename());
			String newFileName = UUID.randomUUID() + "." + ext;

			java.io.File dest = new java.io.File(dir, newFileName);
			file.transferTo(dest);

			String imageUrl = profileUrlPrefix + newFileName;

			userDao.updateProfileImage(userId, imageUrl);

			return imageUrl;
		} catch (Exception e) {
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "이미지 업로드 실패");
		}
	}

	@Override
	public void deleteCurrentUser(String userId, DeleteUserRequest request) {
		User user = userDao.findByUserId(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "존재하지 않는 사용자입니다."));

		String deleteType = request.getDeleteType() != null ? request.getDeleteType() : "USER_REQUEST";
		String deleteDetail = request.getDeleteDetail();

		userDao.softDeleteUser(user.getUserId(), UserStatus.WITHDRAW, deleteType, deleteDetail);
	}

	@Override
	public void restoreUser(String userId) {
		User user = userDao.findByUserIdIncludeDeleted(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));

		if (user.getStatus() != UserStatus.WITHDRAW) {
			throw new BusinessException(ErrorCode.CONFLICT, "탈퇴한 사용자가 아닙니다.");
		}

		userDao.restoreUser(userId);
	}

	private String saveProfileImageFromBase64(String base64) {
		try {
			String[] parts = base64.split(",");
			String dataPart = parts.length > 1 ? parts[1] : parts[0];

			byte[] bytes = java.util.Base64.getDecoder().decode(dataPart);

			java.io.File dir = new java.io.File(profileUploadDir);
			if (!dir.exists()) {
				dir.mkdirs();
			}

			String newFileName = UUID.randomUUID() + ".png";
			java.io.File dest = new java.io.File(dir, newFileName);

			try (java.io.FileOutputStream fos = new java.io.FileOutputStream(dest)) {
				fos.write(bytes);
			}

			return profileUrlPrefix + newFileName;
		} catch (Exception e) {
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "프로필 이미지 처리 중 오류가 발생했습니다.");
		}
	}

	private void ensureNotBlocked(User user) {
		if (user.getStatus() == UserStatus.BLOCK) {
			throw new BusinessException(ErrorCode.ACCOUNT_BLOCKED);
		}
	}

}