package com.moa.common.util;

import java.security.SecureRandom;
import java.util.Base64;

public class JwtSecretGenerator {

	public static void main(String[] args) {
		byte[] key = new byte[32];
		new SecureRandom().nextBytes(key);
		String base64Key = Base64.getEncoder().encodeToString(key);
		System.out.println("JWT SECRET (Base64) = " + base64Key);
	}
}
