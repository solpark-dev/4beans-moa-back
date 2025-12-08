package com.moa.dto.user.response;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import com.moa.domain.OAuthAccount;
import com.moa.domain.User;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UserResponse {

	private String userId;
	private String nickname;
	private String phone;
	private String profileImage;
	private String status;
	private String role;
	private LocalDate regDate;
	private LocalDate lastLoginDate;
	private String loginProvider;
	private List<OAuthConnectionResponse> oauthConnections;
	private Boolean agreeMarketing;
	private Boolean blacklisted;
	private Boolean otpEnabled;
	private String provider;

	public static UserResponse from(User user) {
		return from(user, null);
	}

	public static UserResponse from(User user, List<OAuthAccount> oauthAccounts) {
		List<OAuthConnectionResponse> connections = null;
		if (oauthAccounts != null && !oauthAccounts.isEmpty()) {
			connections = oauthAccounts.stream()
					.map(OAuthConnectionResponse::from)
					.collect(Collectors.toList());
		}

		return UserResponse.builder()
				.userId(user.getUserId())
				.nickname(user.getNickname())
				.phone(user.getPhone())
				.profileImage(user.getProfileImage())
				.status(user.getStatus() != null ? user.getStatus().name() : null)
				.role(user.getRole())
				.regDate(user.getRegDate() != null ? user.getRegDate().toLocalDate() : null)
				.lastLoginDate(user.getLastLoginDate() != null ? user.getLastLoginDate().toLocalDate() : null)
				.loginProvider(null)
				.oauthConnections(connections)
				.agreeMarketing(user.getAgreeMarketing())
				.blacklisted(false)
				.otpEnabled(user.getOtpEnabled())
				.provider(user.getProvider())
				.build();
	}
}
