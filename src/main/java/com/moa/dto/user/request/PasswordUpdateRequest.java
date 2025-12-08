package com.moa.dto.user.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordUpdateRequest {
	private String currentPassword;
	private String newPassword;
	private String newPasswordConfirm;
}
