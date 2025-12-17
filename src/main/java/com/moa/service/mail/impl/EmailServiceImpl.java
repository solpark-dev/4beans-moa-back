package com.moa.service.mail.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.moa.service.mail.EmailService;
import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

	private final Resend resend;

	@Value("${resend.from-address}")
	private String fromAddress;

	@Value("${resend.from-name}")
	private String fromName;

	@Value("${app.email.verify-base-url}")
	private String verifyBaseUrl;

	@Value("${app.email.signup-verify-template:templates/email/signup-verify.html}")
	private String signupVerifyTemplatePath;

	@Override
	public void sendSignupVerificationEmail(String email, String nickname, String token) {
		String verifyUrl = verifyBaseUrl + "?token=" + java.net.URLEncoder.encode(token, StandardCharsets.UTF_8);
		String from = fromName + " <" + fromAddress + ">";

		String template = loadClasspathTemplate(signupVerifyTemplatePath);

		String htmlContent = template.replace("${nickname}", escapeHtml(nickname))
				.replace("${verifyUrl}", escapeHtml(verifyUrl)).replace("{{nickname}}", escapeHtml(nickname))
				.replace("{{verifyUrl}}", escapeHtml(verifyUrl));

		try {
			CreateEmailOptions params = CreateEmailOptions.builder().from(from).to(email).subject("[MOA] 회원가입 이메일 인증")
					.html(htmlContent).build();

			resend.emails().send(params);
		} catch (Exception e) {
			throw new IllegalStateException("이메일 발송 실패", e);
		}
	}

	@Override
	public void sendBankVerificationEmail(String email, String bankName, String maskedAccount, String verifyCode) {
		log.info("[이메일] 계좌 인증 이메일 발송 시작 - 수신자: {}, 은행: {}", email, bankName);

		String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"));
		String depositor = "MOA" + verifyCode;

		String htmlContent = """
				<!DOCTYPE html>
				<html lang="ko">
				<head>
					<meta charset="UTF-8">
					<meta name="viewport" content="width=device-width, initial-scale=1.0">
				</head>
				<body style="margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; background-color: #f5f5f5;">
					<table width="100%%" cellpadding="0" cellspacing="0" style="background-color: #f5f5f5; padding: 40px 20px;">
						<tr>
							<td align="center">
								<table width="100%%" cellpadding="0" cellspacing="0" style="max-width: 480px; background-color: #ffffff; border-radius: 16px; box-shadow: 0 4px 12px rgba(0,0,0,0.1);">
									<!-- Header -->
									<tr>
										<td style="padding: 32px 32px 24px; text-align: center; border-bottom: 1px solid #f0f0f0;">
											<div style="display: inline-block; background: linear-gradient(135deg, #f97316 0%%, #ea580c 100%%); padding: 12px 24px; border-radius: 12px; margin-bottom: 16px;">
												<span style="color: #ffffff; font-size: 24px; font-weight: 800; letter-spacing: -0.5px;">4beans</span>
											</div>
											<h1 style="margin: 0; color: #1e293b; font-size: 20px; font-weight: 700;">입금 알림</h1>
										</td>
									</tr>

									<!-- Content -->
									<tr>
										<td style="padding: 32px;">
											<p style="margin: 0 0 24px; color: #64748b; font-size: 15px; line-height: 1.6; text-align: center;">
												고객님의 계좌로 <strong style="color: #10b981;">1원</strong>이 입금되었습니다.
											</p>

											<!-- 입금 정보 박스 -->
											<div style="background: linear-gradient(135deg, #f0fdf4 0%%, #dcfce7 100%%); border: 2px solid #86efac; border-radius: 16px; padding: 24px; margin-bottom: 24px;">
												<table width="100%%" cellpadding="0" cellspacing="0">
													<tr>
														<td style="padding: 8px 0; color: #64748b; font-size: 13px;">입금 금액</td>
														<td style="padding: 8px 0; text-align: right; color: #10b981; font-size: 18px; font-weight: 700;">+1원</td>
													</tr>
													<tr>
														<td style="padding: 8px 0; color: #64748b; font-size: 13px;">입금자명</td>
														<td style="padding: 8px 0; text-align: right;">
															<span style="color: #1e293b; font-size: 18px; font-weight: 700;">%s</span>
														</td>
													</tr>
													<tr>
														<td style="padding: 8px 0; color: #64748b; font-size: 13px;">입금 일시</td>
														<td style="padding: 8px 0; text-align: right; color: #1e293b; font-size: 14px;">%s</td>
													</tr>
													<tr>
														<td style="padding: 8px 0; color: #64748b; font-size: 13px;">받는 계좌</td>
														<td style="padding: 8px 0; text-align: right; color: #1e293b; font-size: 14px;">%s %s</td>
													</tr>
												</table>
											</div>

											<!-- 인증코드 안내 -->
											<div style="background-color: #fff7ed; border: 2px solid #fed7aa; border-radius: 16px; padding: 20px; text-align: center;">
												<p style="margin: 0 0 8px; color: #9a3412; font-size: 13px; font-weight: 600;">인증 방법</p>
												<p style="margin: 0 0 12px; color: #1e293b; font-size: 14px; line-height: 1.6;">
													입금자명 "<strong>%s</strong>"에서<br>
													숫자 4자리를 입력하세요
												</p>
												<div style="display: inline-block; background: linear-gradient(135deg, #f97316 0%%, #ea580c 100%%); padding: 16px 32px; border-radius: 12px;">
													<span style="color: #ffffff; font-size: 32px; font-weight: 800; letter-spacing: 8px;">%s</span>
												</div>
											</div>

											<!-- 유효시간 -->
											<p style="margin: 24px 0 0; text-align: center; color: #ef4444; font-size: 13px;">
												⏰ 유효시간: 10분
											</p>
										</td>
									</tr>

									<!-- Footer -->
									<tr>
										<td style="padding: 24px 32px; background-color: #f8fafc; border-radius: 0 0 16px 16px; border-top: 1px solid #f0f0f0;">
											<p style="margin: 0; color: #94a3b8; font-size: 12px; text-align: center; line-height: 1.6;">
												※ 이 메일은 계좌 인증을 위한 데모용 메일입니다.<br>
												※ 실제 입금은 발생하지 않습니다.
											</p>
										</td>
									</tr>
								</table>

								<!-- Footer Logo -->
								<p style="margin: 24px 0 0; color: #94a3b8; font-size: 12px; text-align: center;">
									© 2025 4beans MOA. All rights reserved.
								</p>
							</td>
						</tr>
					</table>
				</body>
				</html>
				"""
				.formatted(depositor, currentTime, bankName, maskedAccount, depositor, verifyCode);

		try {
			CreateEmailOptions params = CreateEmailOptions.builder().from(fromAddress).to(email)
					.subject("[4beans] 계좌 인증을 위한 1원이 입금되었습니다").html(htmlContent).build();

			resend.emails().send(params);
			log.info("[이메일] 계좌 인증 이메일 발송 완료 - 수신자: {}", email);
		} catch (Exception e) {
			log.error("[이메일] 계좌 인증 이메일 발송 실패 - 수신자: {}, 오류: {}", email, e.getMessage());
			e.printStackTrace();
		}
	}

	private String loadClasspathTemplate(String path) {
		String normalized = path == null ? "" : path.trim();
		if (normalized.startsWith("classpath:"))
			normalized = normalized.substring("classpath:".length());
		if (normalized.startsWith("/"))
			normalized = normalized.substring(1);

		ClassPathResource resource = new ClassPathResource(normalized);
		if (!resource.exists()) {
			throw new IllegalStateException("이메일 템플릿 로드 실패: " + path);
		}

		try (InputStream in = resource.getInputStream()) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException("이메일 템플릿 로드 실패: " + path, e);
		}
	}

	private String escapeHtml(String value) {
		if (value == null)
			return "";
		String v = value;
		v = v.replace("&", "&amp;");
		v = v.replace("<", "&lt;");
		v = v.replace(">", "&gt;");
		v = v.replace("\"", "&quot;");
		v = v.replace("'", "&#39;");
		return v;
	}
}