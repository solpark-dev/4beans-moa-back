package com.moa.web.admin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moa.common.exception.ApiResponse;
import com.moa.dto.blacklist.AddBlacklistRequest;
import com.moa.dto.blacklist.DeleteBlacklistRequest;
import com.moa.dto.community.response.PageResponse;
import com.moa.dto.user.request.AdminUserSearchRequest;
import com.moa.dto.user.response.AdminUserListItemResponse;
import com.moa.dto.user.response.LoginHistoryResponse;
import com.moa.dto.user.response.UserResponse;
import com.moa.service.auth.LoginHistoryService;
import com.moa.service.blacklist.BlacklistService;
import com.moa.service.user.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserRestController {

	private final UserService userService;
	private final BlacklistService blacklistService;
	private final LoginHistoryService loginHistoryService;

	public AdminUserRestController(UserService userService, BlacklistService blacklistService,
			LoginHistoryService loginHistoryService) {
		this.userService = userService;
		this.blacklistService = blacklistService;
		this.loginHistoryService = loginHistoryService;
	}

	@GetMapping
	public ApiResponse<PageResponse<AdminUserListItemResponse>> list(AdminUserSearchRequest request) {
		return ApiResponse.success(userService.getAdminUserList(request));
	}

	@GetMapping("/{userId}")
	public ApiResponse<UserResponse> detail(@PathVariable String userId) {
		return ApiResponse.success(userService.getUserDetailForAdmin(userId));
	}

	@GetMapping("/{userId}/login-history")
	public ApiResponse<PageResponse<LoginHistoryResponse>> loginHistory(@PathVariable String userId,
			@RequestParam(name = "page", defaultValue = "1") int page,
			@RequestParam(name = "size", defaultValue = "20") int size) {
		return ApiResponse.success(loginHistoryService.getUserLoginHistory(userId, page, size));
	}

	@PostMapping("/blacklist")
	public ApiResponse<Void> addBlacklist(@Valid @RequestBody AddBlacklistRequest request) {
		blacklistService.addBlacklist(request);
		return ApiResponse.success(null);
	}

	@PostMapping("/blacklist/delete")
	public ApiResponse<Void> deleteBlacklist(@Valid @RequestBody DeleteBlacklistRequest request) {
		blacklistService.deleteBlacklist(request);
		return ApiResponse.success(null);
	}
}
