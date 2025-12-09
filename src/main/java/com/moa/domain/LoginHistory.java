package com.moa.domain;

import java.time.LocalDateTime;

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
public class LoginHistory {

	private Long id;
	private String userId;
	private LocalDateTime loginAt;
	private Boolean success;
	private String loginIp;
	private String userAgent;
	private String failReason;
	private String loginType;
}
