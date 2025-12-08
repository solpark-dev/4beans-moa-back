package com.moa.service.mail;

public interface EmailService {
	void sendSignupVerificationEmail(String email, String nickname, String token);
}