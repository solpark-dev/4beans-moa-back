package com.moa.service.passauth;

import java.util.Map;

public interface PassAuthService {
	Map<String, Object> requestCertification();

	Map<String, Object> verifyCertification(String impUid) throws Exception;
}