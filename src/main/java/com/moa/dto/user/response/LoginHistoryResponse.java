package com.moa.dto.user.response;

import java.time.LocalDateTime;

import com.moa.domain.LoginHistory;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginHistoryResponse {

	private LocalDateTime loginAt;
	private boolean success;
	private String loginIp;
	private String userAgent;
	private String loginType;
	private String failReason;

	public static LoginHistoryResponse from(LoginHistory history) {
		return LoginHistoryResponse.builder().loginAt(history.getLoginAt())
				.success(history.getSuccess() != null && history.getSuccess() == 1).loginIp(history.getLoginIp())
				.userAgent(history.getUserAgent()).loginType(history.getLoginType()).failReason(history.getFailReason())
				.build();
	}
}
