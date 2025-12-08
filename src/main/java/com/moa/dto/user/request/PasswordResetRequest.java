package com.moa.dto.user.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordResetRequest {
	private String token;
	private String password;
	private String passwordConfirm;
}
