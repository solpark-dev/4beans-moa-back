package com.moa.service.auth;

import com.moa.dto.auth.OtpSetupResponse;
import com.moa.dto.auth.OtpVerifyRequest;

public interface OtpService {

	OtpSetupResponse setup();

	void verify(OtpVerifyRequest request);

	void disable();

	void verifyLoginCode(String userId, String code);
	
	void disableWithCode(String userId, String code);
}
