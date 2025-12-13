package com.moa.service.mail;

public interface EmailService {
	void sendSignupVerificationEmail(String email, String nickname, String token);

	/**
	 * 계좌 인증 이메일 발송 (1원 입금 알림)
	 * @param email 수신자 이메일
	 * @param bankName 은행명
	 * @param maskedAccount 마스킹된 계좌번호
	 * @param verifyCode 4자리 인증코드
	 */
	void sendBankVerificationEmail(String email, String bankName, String maskedAccount, String verifyCode);
}