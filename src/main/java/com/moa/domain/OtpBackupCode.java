package com.moa.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpBackupCode {

	private Long id;
	private String userId;
	private String codeHash;
	private boolean used;
	private LocalDateTime createdAt;
	private LocalDateTime usedAt;
}
