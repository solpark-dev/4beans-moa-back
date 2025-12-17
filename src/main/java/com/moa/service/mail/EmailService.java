package com.moa.service.mail;

public interface EmailService {
	void sendSignupVerificationEmail(String email, String nickname, String token);

	void sendBankVerificationEmail(String email, String bankName, String maskedAccount, String verifyCode);
}