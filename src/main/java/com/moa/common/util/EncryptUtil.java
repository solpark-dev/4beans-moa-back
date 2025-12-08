package com.moa.common.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class EncryptUtil {

	private static final PasswordEncoder encoder = new BCryptPasswordEncoder();

	public static String encode(String raw) {
		return encoder.encode(raw);
	}

	public static boolean matches(String raw, String encoded) {
		return encoder.matches(raw, encoded);
	}
}
