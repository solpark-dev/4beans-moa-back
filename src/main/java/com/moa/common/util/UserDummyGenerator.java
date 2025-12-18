package com.moa.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class UserDummyGenerator {

	private static final int USER_COUNT = 100;
	private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private static final String[] DOMAINS = { "naver.com", "gmail.com", "daum.net", "nate.com" };

	private static final String[] PROVIDERS = { "LOCAL", "KAKAO", "GOOGLE" };

	private static final Random R = new Random();

	public static void main(String[] args) {

		Set<String> emails = new HashSet<>();
		Set<String> phones = new HashSet<>();

		String passwordHash = EncryptUtil.encode("1234");

		System.out.println("INSERT INTO USERS (");
		System.out.println("    USER_ID, PASSWORD, NICKNAME, PHONE,");
		System.out.println("    PROFILE_IMAGE, ROLE, USER_STATUS, REG_DATE,");
		System.out.println("    CI, PASS_CERTIFIED_AT, LAST_LOGIN_DATE,");
		System.out.println("    LOGIN_FAIL_COUNT, UNLOCK_SCHEDULED_AT,");
		System.out.println("    DELETE_DATE, DELETE_TYPE, DELETE_DETAIL,");
		System.out.println("    AGREE_MARKETING, PROVIDER, OTP_SECRET, OTP_ENABLED");
		System.out.println(") VALUES");

		for (int i = 1; i <= USER_COUNT; i++) {

			String email = generateEmail(emails);
			String phone = generatePhone(phones);
			String nickname = "유저_" + randomString(6);
			String ci = "CI_" + randomString(12);

			String provider = PROVIDERS[R.nextInt(PROVIDERS.length)];
			int agreeMarketing = R.nextBoolean() ? 1 : 0;

			LocalDateTime reg = LocalDateTime.of(2024, 3, 1, 9, 0).plusDays(i).plusMinutes(R.nextInt(600));

			LocalDateTime login = reg.plusDays(R.nextInt(200));

			System.out.println("(");
			System.out.println(" '" + email + "',");
			System.out.println(" '" + passwordHash + "',");
			System.out.println(" '" + nickname + "',");
			System.out.println(" '" + phone + "',");
			System.out.println(" NULL, 'USER', 'ACTIVE',");
			System.out.println(" '" + reg.format(F) + "',");
			System.out.println(" '" + ci + "',");
			System.out.println(" '" + reg.format(F) + "',");
			System.out.println(" '" + login.format(F) + "',");
			System.out.println(" 0, NULL, NULL, NULL, NULL,");
			System.out.println(" " + agreeMarketing + ", '" + provider + "', NULL, 0");
			System.out.print(")");

			if (i < USER_COUNT) {
				System.out.println(",");
			}
		}

		System.out.println(";");
	}

	private static String generateEmail(Set<String> used) {
		while (true) {
			String email = randomString(8) + "@" + DOMAINS[R.nextInt(DOMAINS.length)];
			if (used.add(email)) {
				return email;
			}
		}
	}

	private static String generatePhone(Set<String> used) {
		while (true) {
			String phone = "010" + (10000000 + R.nextInt(90000000));
			if (used.add(phone)) {
				return phone;
			}
		}
	}

	private static String randomString(int len) {
		String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < len; i++) {
			sb.append(chars.charAt(R.nextInt(chars.length())));
		}
		return sb.toString();
	}
}
