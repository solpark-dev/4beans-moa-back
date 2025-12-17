package com.moa.common.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AESUtil {

	private static String secretKey;

	@Value("${app.security.aes-key}")
	public void setSecretKey(String key) {
		AESUtil.secretKey = key;
	}

	public static String encrypt(String plainText) {
		if (plainText == null)
			return null;
		try {
			SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "AES");
			Cipher cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
			byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(encryptedBytes);
		} catch (Exception e) {
			log.error("Encryption failed", e);
			throw new RuntimeException("Encryption failed", e);
		}
	}

	public static String decrypt(String encryptedText) {
		if (encryptedText == null)
			return null;
		try {
			SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "AES");
			Cipher cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
			byte[] decodedBytes = Base64.getDecoder().decode(encryptedText);
			return new String(cipher.doFinal(decodedBytes), StandardCharsets.UTF_8);
		} catch (Exception e) {
			log.warn("Decryption failed, returning original text (might be plain text): {}", e.getMessage());
			return encryptedText;
		}
	}
}
