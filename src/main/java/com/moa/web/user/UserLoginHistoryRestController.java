package com.moa.web.user;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moa.common.exception.ApiResponse;
import com.moa.dto.auth.TokenResponse;
import com.moa.dto.community.response.PageResponse;
import com.moa.dto.user.response.LoginHistoryResponse;
import com.moa.service.auth.LoginHistoryService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users/login-history")
@RequiredArgsConstructor
public class UserLoginHistoryRestController {

	private final LoginHistoryService loginHistoryService;

	@GetMapping("/me")
	public ApiResponse<PageResponse<LoginHistoryResponse>> getMyLoginHistory(
			@RequestParam(name = "page", defaultValue = "1") int page,
			@RequestParam(name = "size", defaultValue = "20") int size) {
		return ApiResponse.success(loginHistoryService.getMyLoginHistory(page, size));
	}

}
