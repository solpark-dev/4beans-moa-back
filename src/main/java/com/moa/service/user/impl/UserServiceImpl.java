package com.moa.service.user.impl;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.moa.auth.provider.JwtProvider;
import com.moa.common.event.UserDeletedEvent;
import com.moa.common.exception.BusinessException;
import com.moa.common.exception.ErrorCode;
import com.moa.dao.admin.AdminDao;
import com.moa.dao.oauth.OAuthAccountDao;
import com.moa.dao.user.EmailVerificationDao;
import com.moa.dao.user.UserCardDao;
import com.moa.dao.user.UserDao;
import com.moa.domain.EmailVerification;
import com.moa.domain.OAuthAccount;
import com.moa.domain.User;
import com.moa.domain.enums.UserStatus;
import com.moa.dto.auth.TokenResponse;
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

	private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
	private final UserDao userDao;
	private final OAuthAccountDao oauthAccountDao;
	private final EmailVerificationDao emailVerificationDao;
	private final EmailService emailService;
	private final PasswordEncoder passwordEncoder;
	private final OAuthAccountService oauthAccountService;
	private final UserCardDao userCardDao;
	private final AdminDao adminDao;
	private final UserAddValidator userAddValidator;
	private final JwtProvider jwtProvider;
	private final ApplicationEventPublisher eventPublisher;

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

		boolean isSocial = request.getProvider() != null && request.getProviderUserId() != null;

		if (isSocial) {
			userAddValidator.validateForSocialSignup(request);
		} else {
			userAddValidator.validateForSignup(request);
		}

		String userId = request.getUserId().toLowerCase();

		if (userDao.existsByUserId(userId) > 0) {
			throw new BusinessException(ErrorCode.DUPLICATED_USER, "이미 가입된 이메일입니다.");
		}

		if (userDao.existsByPhone(request.getPhone()) > 0) {
			throw new BusinessException(ErrorCode.DUPLICATED_PHONE, "이미 가입된 휴대폰 번호입니다.");
		}

		String encodedPassword = isSocial ? passwordEncoder.encode(UUID.randomUUID().toString())
				: passwordEncoder.encode(request.getPassword());

		User user = User.builder().userId(userId).password(encodedPassword).nickname(request.getNickname())
				.phone(request.getPhone()).role("USER").status(isSocial ? UserStatus.ACTIVE : UserStatus.PENDING)
				.provider(isSocial ? request.getProvider() : null).ci(request.getCi()).build();

		userDao.insertUser(user);

		if (isSocial) {
			OAuthAccount account = OAuthAccount.builder().oauthId(UUID.randomUUID().toString())
					.provider(request.getProvider()).providerUserId(request.getProviderUserId()).userId(userId).build();

			oauthAccountService.addOAuthAccount(account);
			return UserResponse.from(user);
		}

		String token = UUID.randomUUID().toString();
		LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(30);

		emailVerificationDao.expirePreviousTokens(userId);

		EmailVerification verification = EmailVerification.builder().userId(userId).token(token).expiresAt(expiresAt)
				.build();

		emailVerificationDao.insert(verification);

		emailService.sendSignupVerificationEmail(userId, user.getNickname(), token);

		return UserResponse.from(user);
	}

	@Override
	@Transactional
	public Map<String, Object> addUserAndLogin(UserCreateRequest request) {

		UserResponse user = addUser(request);

		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(user.getUserId(),
				null, List.of(() -> "ROLE_USER"));

		TokenResponse token = jwtProvider.generateToken(authentication);

		return Map.of("signupType", "SOCIAL", "user", user, "accessToken", token.getAccessToken(), "refreshToken",
				token.getRefreshToken(), "accessTokenExpiresIn", token.getAccessTokenExpiresIn());

	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<AdminUserListItemResponse> getAdminUserList(AdminUserSearchRequest request) {
		int page = request.getPageOrDefault();
		int size = request.getSizeOrDefault();
		int offset = (page - 1) * size;

		request.setSize(size);
		request.setOffset(offset);

		List<String> dbSortList = new java.util.ArrayList<>();

		if (request.getSort() != null && !request.getSort().isBlank()) {
			String[] sortParams = request.getSort().split(",");

			for (int i = 0; i < sortParams.length; i += 2) {
				if (i + 1 >= sortParams.length)
					break;

				String property = sortParams[i];
				String direction = sortParams[i + 1].toUpperCase();

				if (!"ASC".equals(direction) && !"DESC".equals(direction)) {
					direction = "DESC";
				}

				String dbColumn = null;
				switch (property) {
				case "lastLoginDate":
					dbColumn = "u.LAST_LOGIN_DATE";
					break;
				case "regDate":
					dbColumn = "u.REG_DATE";
					break;
				case "userId":
					dbColumn = "u.USER_ID";
					break;
				case "status":
					dbColumn = "u.USER_STATUS";
					break;
				default:
					continue;
				}

				if (dbColumn != null) {
					dbSortList.add(dbColumn + " " + direction);
				}
			}
		}

		request.setDbSortList(dbSortList);

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
		boolean hasBillingKey = userCardDao.findByUserId(userId).isPresent();

		return UserResponse.from(user, oauthAccounts, hasBillingKey);
	}

	@Override
	public String findUserIdByPhone(String phone) {
		return userDao.findUserIdByPhone(phone)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "해당 휴대폰 번호로 가입된 계정이 없습니다."));
	}

	@Override
	public Optional<User> findByPhone(String phone) {
		return userDao.findByPhone(phone);
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

		if (!user.getNickname().equals(request.getNickname())) {
			if (userDao.existsByNicknameExceptMe(request.getNickname(), userId) > 0) {
				throw new BusinessException(ErrorCode.CONFLICT, "이미 사용 중인 닉네임입니다.");
			}
		}
		String profileImage = user.getProfileImage();
		if (request.getProfileImage() != null && request.getProfileImage().startsWith("/uploads/")) {
			profileImage = request.getProfileImage();
		}

		User updated = User.builder().userId(user.getUserId()).password(user.getPassword())
				.nickname(request.getNickname()).phone(request.getPhone()).profileImage(profileImage)
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
			File dir = new File(profileUploadDir);
			if (!dir.exists()) {
				boolean created = dir.mkdirs();
				if (!created) {
					throw new BusinessException(ErrorCode.INTERNAL_ERROR, "업로드 디렉토리 생성 실패");
				}
			}

			String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
			if (ext == null || ext.isBlank()) {
				ext = "png";
			}

			String newFileName = UUID.randomUUID() + "." + ext;
			File dest = new File(dir, newFileName);

			file.transferTo(dest);

			String imageUrl = combinePath(profileUrlPrefix, newFileName);
			userDao.updateProfileImage(userId, imageUrl);

			return imageUrl;

		} catch (Exception e) {
			log.error("프로필 이미지 업로드 실패 userId={}, path={}", userId, profileUploadDir, e);
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "이미지 업로드 실패");
		}
	}

	@Override
	public void deleteCurrentUser(String userId, DeleteUserRequest request) {
		User user = userDao.findByUserId(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "존재하지 않는 사용자입니다."));

		String deleteType = request.getDeleteType() != null ? request.getDeleteType() : "USER_REQUEST";
		String deleteDetail = request.getDeleteDetail();

		eventPublisher.publishEvent(UserDeletedEvent.of(userId, deleteType, deleteDetail));

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

	@Override
	public void unlockByCertification(String userId, String phone, String ci) {
		User user = userDao.findByUserIdIncludeDeleted(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		if (user.getStatus() == UserStatus.WITHDRAW) {
			throw new BusinessException(ErrorCode.ACCOUNT_WITHDRAW);
		}

		if (adminDao.findActiveBlacklistByUserId(userId) != null) {
			throw new BusinessException(ErrorCode.ACCOUNT_BLOCKED);
		}

		if (user.getPhone() == null || !user.getPhone().equals(phone)) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED, "본인인증 정보가 계정 정보와 일치하지 않습니다.");
		}

		if (user.getCi() != null && ci != null && !user.getCi().equals(ci)) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED, "본인인증 정보가 계정 정보와 일치하지 않습니다.");
		}

		if (user.getStatus() != UserStatus.BLOCK) {
			throw new BusinessException(ErrorCode.CONFLICT, "잠금 상태의 계정이 아닙니다.");
		}

		userDao.updateUserStatus(userId, UserStatus.ACTIVE);
		userDao.resetLoginFailCount(userId);
	}

	private void ensureNotBlocked(User user) {
		if (user.getStatus() == UserStatus.BLOCK) {
			throw new BusinessException(ErrorCode.ACCOUNT_BLOCKED);
		}
	}

	private String combinePath(String prefix, String suffix) {
		if (prefix == null)
			prefix = "";
		if (suffix == null)
			suffix = "";

		boolean prefixHasSlash = prefix.endsWith("/") || prefix.endsWith("\\");
		boolean suffixHasSlash = suffix.startsWith("/") || suffix.startsWith("\\");

		if (prefixHasSlash && suffixHasSlash) {
			return prefix + suffix.substring(1);
		} else if (!prefixHasSlash && !suffixHasSlash) {
			return prefix + "/" + suffix;
		} else {
			return prefix + suffix;
		}
	}

}