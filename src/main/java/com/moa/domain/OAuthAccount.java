package com.moa.domain;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class OAuthAccount {
	private String oauthId;
	private String provider;
	private String providerUserId;
	private String userId;
	private LocalDateTime connectedDate;
	private LocalDateTime releaseDate;
}
