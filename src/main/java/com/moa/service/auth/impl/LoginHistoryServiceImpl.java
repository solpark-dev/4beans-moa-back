package com.moa.service.auth.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moa.common.exception.BusinessException;
import com.moa.common.exception.ErrorCode;
import com.moa.dao.auth.LoginHistoryDao;
import com.moa.domain.LoginHistory;
import com.moa.dto.community.response.PageResponse;
import com.moa.dto.user.response.LoginHistoryResponse;
import com.moa.service.auth.LoginHistoryService;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class LoginHistoryServiceImpl implements LoginHistoryService {

	private final LoginHistoryDao loginHistoryDao;

	@Override
	public void recordSuccess(String userId, String loginType, String loginIp, String userAgent) {
		if (userId == null || userId.isBlank()) {
			return;
		}

		LoginHistory history = LoginHistory.builder().userId(userId.toLowerCase()).loginAt(LocalDateTime.now())
				.success(1)
				.loginIp(loginIp).userAgent(userAgent).failReason(null).loginType(loginType).build();

		loginHistoryDao.insert(history);
	}

	@Override
	public void recordFailure(String userId, String loginType, String loginIp, String userAgent, String failReason) {
	    if (userId == null || userId.isBlank()) {
	        return;
	    }

	    LoginHistory history = LoginHistory.builder()
	        .userId(userId.toLowerCase())
	        .loginAt(LocalDateTime.now())
	        .success(0)
	        .loginIp(loginIp)
	        .userAgent(userAgent)
	        .failReason(failReason)
	        .loginType(loginType)
	        .build();

	    loginHistoryDao.insert(history);
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<LoginHistoryResponse> getMyLoginHistory(int page, int size) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null || !authentication.isAuthenticated()
				|| "anonymousUser".equals(authentication.getPrincipal())) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED, "로그인 정보가 없습니다.");
		}

		String userId = authentication.getName();

		return getUserLoginHistory(userId, page, size);
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<LoginHistoryResponse> getUserLoginHistory(String userId, int page, int size) {
		if (page < 1) {
			page = 1;
		}
		if (size < 1) {
			size = 10;
		}

		int offset = (page - 1) * size;

		long total = loginHistoryDao.countByUserId(userId);
		if (total == 0) {
			return new PageResponse<>(List.of(), page, size, 0);
		}

		List<LoginHistory> list = loginHistoryDao.findByUserId(userId, offset, size);
		List<LoginHistoryResponse> content = list.stream().map(LoginHistoryResponse::from).collect(Collectors.toList());

		return new PageResponse<>(content, page, size, total);
	}
}
