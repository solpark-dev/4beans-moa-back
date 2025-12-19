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
	private Boolean hasBillingKey;
	private String provider;

	public static UserResponse from(User user) {
		return from(user, null, false);
	}

	public static UserResponse from(User user, List<OAuthAccount> oauthAccounts) {
		return from(user, oauthAccounts, false);
	}

	public static UserResponse from(
	        User user,
	        List<OAuthAccount> oauthAccounts,
	        Boolean hasBillingKey
	) {
	    List<OAuthConnectionResponse> connections = null;

	    if (oauthAccounts != null && !oauthAccounts.isEmpty()) {
	        connections = oauthAccounts.stream()
	                .map(OAuthConnectionResponse::from)
	                .collect(Collectors.toList());
	    }

	    String loginProvider =
	            oauthAccounts != null
	                    ? oauthAccounts.stream()
	                        .filter(o -> o.getReleaseDate() == null)
	                        .map(OAuthAccount::getProvider)
	                        .findFirst()
	                        .orElse("email")
	                    : "email";

	    return UserResponse.builder()
	            .userId(user.getUserId())
	            .nickname(user.getNickname())
	            .phone(user.getPhone())
	            .profileImage(user.getProfileImage())
	            .status(user.getStatus() != null ? user.getStatus().name() : null)
	            .role(user.getRole())
	            .regDate(user.getRegDate() != null ? user.getRegDate().toLocalDate() : null)
	            .lastLoginDate(user.getLastLoginDate() != null ? user.getLastLoginDate().toLocalDate() : null)
	            .loginProvider(loginProvider)
	            .oauthConnections(connections)
	            .agreeMarketing(user.getAgreeMarketing())
	            .blacklisted(false)
	            .otpEnabled(user.getOtpEnabled())
	            .hasBillingKey(hasBillingKey)
	            .build();
	}


}
