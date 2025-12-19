package com.moa.web.user;

import java.util.Map;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.moa.common.exception.ApiResponse;
import com.moa.common.exception.BusinessException;
import com.moa.common.exception.ErrorCode;
import com.moa.dao.account.AccountDao;
import com.moa.dao.user.UserCardDao;
import com.moa.domain.Account;
import com.moa.domain.UserCard;
import com.moa.dto.user.request.DeleteUserRequest;
import com.moa.dto.user.request.PasswordConfirmRequest;
import com.moa.dto.user.request.PasswordFormatCheckRequest;
import com.moa.dto.user.request.PasswordResetRequest;
import com.moa.dto.user.request.PasswordResetStartRequest;
import com.moa.dto.user.request.PasswordUpdateRequest;
import com.moa.dto.user.request.UserUpdateRequest;
import com.moa.dto.user.response.UserResponse;
import com.moa.service.oauth.OAuthAccountService;
import com.moa.service.user.UserService;

@RestController
@RequestMapping(value = "/api/users")
public class UserRestController {

	private final UserService userService;
	private final AccountDao accountDao;
	private final UserCardDao userCardDao;
	private final OAuthAccountService oauthService;

	private final com.moa.service.payment.TossPaymentService tossPaymentService;

	public UserRestController(UserService userService, AccountDao accountDao, UserCardDao userCardDao,
			com.moa.service.payment.TossPaymentService tossPaymentService, OAuthAccountService oauthService) {
		this.userService = userService;
		this.accountDao = accountDao;
		this.userCardDao = userCardDao;
		this.oauthService = oauthService;
		this.tossPaymentService = tossPaymentService;

	}

	private String getCurrentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return null;
		}
		Object principal = authentication.getPrincipal();
		if (principal == null || "anonymousUser".equals(principal)) {
			return null;
		}
		return authentication.getName();
	}

	@GetMapping("/me")
	public ApiResponse<UserResponse> me() {
		String userId = getCurrentUserId();
		if (userId == null) {
			return ApiResponse.success(null);
		}
		return ApiResponse.success(userService.getCurrentUser());
	}

	@PostMapping("/delete")
	public ApiResponse<Void> delete(@RequestBody DeleteUserRequest request) {
		String userId = getCurrentUserId();
		if (userId == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
		}
		userService.deleteCurrentUser(userId, request);
		return ApiResponse.success(null);
	}

	@PostMapping("/resetPwd/start")
	public ApiResponse<Void> startResetPwd(@RequestBody PasswordResetStartRequest request) {
		userService.startPasswordReset(request);
		return ApiResponse.success(null);
	}

	@PostMapping("/resetPwd")
	public ApiResponse<Void> resetPwd(@RequestBody PasswordResetRequest request) {
		userService.resetPassword(request);
		return ApiResponse.success(null);
	}

	@PostMapping("/updatePwd")
	public ApiResponse<Void> updatePwd(@RequestBody PasswordUpdateRequest request) {
		String userId = getCurrentUserId();
		if (userId == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
		}
		userService.updatePassword(userId, request);
		return ApiResponse.success(null);
	}

	@PostMapping("/update")
	public ApiResponse<UserResponse> update(@RequestBody UserUpdateRequest request) {
		String userId = getCurrentUserId();
		if (userId == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
		}
		return ApiResponse.success(userService.updateUser(userId, request));
	}

	@PostMapping("/uploadProfileImage")
	public ApiResponse<String> uploadProfileImage(@RequestParam("file") MultipartFile file) {
		String userId = getCurrentUserId();
		if (userId == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
		}
		return ApiResponse.success(userService.uploadProfileImage(userId, file));
	}

	@PostMapping("/checkPasswordFormat")
	public ApiResponse<Void> checkPasswordFormat(@RequestBody PasswordFormatCheckRequest request) {
		userService.validatePasswordRule(request.getPassword());
		return ApiResponse.success(null);
	}

	@PostMapping("/checkPasswordConfirm")
	public ApiResponse<Void> checkPasswordConfirm(@RequestBody PasswordConfirmRequest request) {
		userService.validatePasswordConfirm(request.getPassword(), request.getPasswordConfirm());
		return ApiResponse.success(null);
	}

	@PostMapping("/checkCurrentPassword")
	public ApiResponse<Void> checkCurrentPassword(@RequestBody PasswordUpdateRequest request) {
		String userId = getCurrentUserId();
		if (userId == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
		}
		userService.checkCurrentPassword(userId, request.getCurrentPassword());
		return ApiResponse.success(null);
	}

	@GetMapping("/check-nickname")
	public ApiResponse<Map<String, Object>> checkNickname(@RequestParam String nickname) {
		boolean exists = userService.existsByNickname(nickname);
		return ApiResponse.success(Map.of("nickname", nickname, "available", !exists));
	}

	@GetMapping("/me/account")
	public ApiResponse<Account> getMyAccount() {
		String userId = getCurrentUserId();
		if (userId == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
		}
		return ApiResponse.success(accountDao.findByUserId(userId).orElse(null));
	}

	@GetMapping("/me/card")
	public ApiResponse<UserCard> getMyCard() {
		String userId = getCurrentUserId();
		if (userId == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
		}
		return ApiResponse.success(userCardDao.findByUserId(userId).orElse(null));
	}

	@PostMapping("/me/card")
	public ApiResponse<UserCard> registerCard(@RequestBody Map<String, String> request) {
		String userId = getCurrentUserId();
		if (userId == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
		}

		String billingKey = request.get("billingKey");
		String cardCompany = request.get("cardCompany");
		String cardNumber = request.get("cardNumber");

		if (billingKey == null || billingKey.isEmpty()) {
			throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "빌링키가 필요합니다.");
		}

		userCardDao.deleteUserCard(userId);

		UserCard userCard = UserCard.builder().userId(userId).billingKey(billingKey).cardCompany(cardCompany)
				.cardNumber(cardNumber).regDate(java.time.LocalDateTime.now()).build();

		userCardDao.insertUserCard(userCard);

		return ApiResponse.success(userCard);
	}

	@PostMapping("/me/billing-key/issue")
	@Transactional
	@SuppressWarnings("unchecked")
	public ApiResponse<UserCard> issueBillingKey(@RequestBody Map<String, String> request) {
		String userId = getCurrentUserId();
		if (userId == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
		}

		String authKey = request.get("authKey");
		if (authKey == null || authKey.isEmpty()) {
			throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "인증키가 필요합니다.");
		}

		Map<String, Object> billingData = tossPaymentService.issueBillingKey(authKey, userId);

		String billingKey = (String) billingData.get("billingKey");
		// 토스페이먼츠 응답에서 cardCompany는 최상위에 있음
		String cardCompany = (String) billingData.get("cardCompany");
		// cardNumber도 최상위 또는 card 객체에서 가져올 수 있음
		String cardNumber = (String) billingData.get("cardNumber");
		if (cardNumber == null) {
			Map<String, Object> cardInfo = (Map<String, Object>) billingData.get("card");
			if (cardInfo != null) {
				cardNumber = (String) cardInfo.get("number");
			}
		}

		UserCard newUserCard = UserCard.builder().userId(userId).billingKey(billingKey)
				.cardCompany(cardCompany != null ? cardCompany : "카드")
				.cardNumber(cardNumber != null ? cardNumber : "****").regDate(java.time.LocalDateTime.now()).build();

		try {
			Optional<UserCard> existingUserCard = userCardDao.findByUserId(userId);
			if (existingUserCard.isPresent()) {
				userCardDao.updateUserCard(newUserCard);
			} else {
				userCardDao.insertUserCard(newUserCard);
			}
		} catch (Exception e) {
			// DB 저장 실패 시 로깅 및 예외 처리
			// 참고: 빌링키는 토스페이먼츠에 이미 발급됨. 수동 처리 필요할 수 있음.
			System.err.println("카드 정보 저장 실패 - userId: " + userId + ", error: " + e.getMessage());
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "카드 정보 저장에 실패했습니다. 다시 시도해주세요.");
		}

		return ApiResponse.success(newUserCard);
	}

	@PostMapping("/me/oauth/connect")
	public ApiResponse<Void> connectSocialAccount(@RequestBody Map<String, String> request) {

		String userId = getCurrentUserId();
		if (userId == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
		}

		String provider = request.get("provider");
		String providerUserId = request.get("providerUserId");

		if (provider == null || providerUserId == null) {
			throw new BusinessException(ErrorCode.INVALID_PARAMETER, "provider 또는 providerUserId 누락");
		}

		oauthService.connectOAuthAccount(userId, provider, providerUserId);

		return ApiResponse.success(null);
	}

	@org.springframework.web.bind.annotation.DeleteMapping("/me/account")
	public ApiResponse<Void> deleteMyAccount() {
		String userId = getCurrentUserId();
		if (userId == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
		}
		accountDao.deleteByUserId(userId);
		return ApiResponse.success(null);
	}

	@org.springframework.web.bind.annotation.DeleteMapping("/me/card")
	public ApiResponse<Void> deleteMyCard() {
		String userId = getCurrentUserId();
		if (userId == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
		}
		userCardDao.deleteUserCard(userId);
		return ApiResponse.success(null);
	}

}