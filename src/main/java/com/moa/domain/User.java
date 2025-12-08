package com.moa.domain;

import java.time.LocalDateTime;

import com.moa.domain.enums.UserStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

	private String userId;
	private String password;
	private String nickname;
	private String phone;
	private String profileImage;
	private String role;
	private UserStatus status;
	private LocalDateTime regDate;
	private String ci;
	private LocalDateTime passCertifiedAt;
	private LocalDateTime lastLoginDate;
	private int loginFailCount;
	private LocalDateTime unlockScheduledAt;
	private LocalDateTime deleteDate;
	private String deleteType;
	private String deleteDetail;
	private Boolean agreeMarketing;
	private String otpSecret;
	private Boolean otpEnabled;
	private String provider;
}
