package com.moa.service.mail.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.moa.service.mail.EmailService;
import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

	private final Resend resend;

	@Value("${resend.from-address}")
	private String fromAddress;

	@Value("${app.email.verify-base-url}")
	private String verifyBaseUrl;

	@Override
	public void sendSignupVerificationEmail(String email, String nickname, String token) {
		String verifyUrl = verifyBaseUrl + "?token=" + token;

		String htmlContent = "<div>" + "<h1>MOA 회원가입 인증</h1>" + "<p>" + nickname + "님, 가입을 환영합니다.</p>"
				+ "<p>아래 링크를 클릭하여 인증을 완료해주세요:</p>" + "<a href='" + verifyUrl + "'>이메일 인증하기</a>" + "</div>";

		try {
			CreateEmailOptions params = CreateEmailOptions.builder().from(fromAddress).to(email)
					.subject("[MOA] 회원가입 이메일 인증").html(htmlContent).build();

			resend.emails().send(params);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}