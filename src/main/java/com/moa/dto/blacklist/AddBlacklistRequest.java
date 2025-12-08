package com.moa.dto.blacklist;

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
public class AddBlacklistRequest {
	private String userId;
	private String reasonType;
	private String reasonDetail;
}
