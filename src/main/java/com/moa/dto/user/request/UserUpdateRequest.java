package com.moa.dto.user.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateRequest {
	private String nickname;
	private String phone;
	private String profileImage;
	private boolean agreeMarketing;
}
