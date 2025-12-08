package com.moa.dto.user.response;

import java.time.LocalDate;

import com.moa.domain.OAuthAccount;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OAuthConnectionResponse {

	private String oauthId;
	private String provider;
	private String providerUserId;
	private LocalDate connectedDate;
	private LocalDate releaseDate;

	public static OAuthConnectionResponse from(OAuthAccount account) {
		return OAuthConnectionResponse.builder().oauthId(account.getOauthId()).provider(account.getProvider())
				.providerUserId(account.getProviderUserId())
				.connectedDate(account.getConnectedDate() != null ? account.getConnectedDate().toLocalDate() : null)
				.releaseDate(account.getReleaseDate() != null ? account.getReleaseDate().toLocalDate() : null).build();
	}
}
