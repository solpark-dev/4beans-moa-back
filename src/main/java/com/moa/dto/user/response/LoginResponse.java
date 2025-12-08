package com.moa.dto.user.response;

import com.moa.domain.User;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {
	private String userId;
	private String nickname;
	private String token;

	public static LoginResponse from(User user, String token) {
		return LoginResponse.builder().userId(user.getUserId()).nickname(user.getNickname()).token(token).build();
	}
}
