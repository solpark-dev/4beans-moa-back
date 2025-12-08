package com.moa.dto.user.response;

import java.time.LocalDateTime;

import com.moa.domain.LoginHistory;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginHistoryResponse {

	private LocalDateTime loginAt;
	private Boolean success;
	private String loginIp;
	private String userAgent;
	private String loginType;
	private String failReason;

	public static LoginHistoryResponse from(LoginHistory history) {
		return LoginHistoryResponse.builder().loginAt(history.getLoginAt()).success(history.getSuccess())
				.loginIp(history.getLoginIp()).userAgent(history.getUserAgent()).loginType(history.getLoginType())
				.failReason(history.getFailReason()).build();
	}
}
