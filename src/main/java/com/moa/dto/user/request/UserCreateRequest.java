package com.moa.dto.user.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
public class UserCreateRequest {

	@NotBlank(message = "아이디(이메일)를 입력해 주세요.")
	private String userId;

	private String password;

	private String passwordConfirm;

	@NotBlank(message = "닉네임을 입력해 주세요.")
	@Pattern(regexp = "^[A-Za-z0-9가-힣]{2,10}$", message = "닉네임은 2~10자, 한글/영문/숫자만 가능합니다.")
	private String nickname;

	@NotBlank(message = "휴대폰 번호를 입력해 주세요.")
	private String phone;

	@NotBlank(message = "본인인증 정보(ci)가 누락되었습니다.")
	private String ci;

	private boolean agreeMarketing;

	private String profileImageBase64;

	private String provider;

	private String providerUserId;
}
