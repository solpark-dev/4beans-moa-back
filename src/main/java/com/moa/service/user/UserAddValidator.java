package com.moa.service.user;

import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.moa.common.exception.BusinessException;
import com.moa.common.exception.ErrorCode;
import com.moa.dao.user.UserDao;
import com.moa.dto.user.request.UserCreateRequest;

@Component
public class UserAddValidator {

	private static final Pattern PASSWORD_PATTERN = Pattern
			.compile("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()])[A-Za-z\\d!@#$%^&*()]{8,20}$");

	private static final List<String> BAD_WORDS = List.of(
			// 영어 욕설
			"fuck", "shit", "bitch", "asshole", "damn", "hell", "crap", "bastard", "dick", "pussy", "cunt",
			"motherfucker", "fucker", "douchebag", "wanker", "bollocks", "tits", "cock", "ass", "idiot", "moron",
			"loser", "jerk", "dolt", "dumb", "crappy", "goddamn", "scumbag", "slut", "whore", "piss", "suck", "balls",
			"bellend", "twat", "bloody", "sod", "son of a bitch",

			// 한국어 욕설(기본)
			"개새", "개새끼", "씨발", "시발", "좆", "병신", "썅", "새끼", "니미", "염병", "지랄", "닥쳐", "미친", "걸레", "등신", "또라이", "호로", "후레",
			"창녀", "보지", "자지", "대가리", "아가리", "개년", "개놈", "개소리", "꺼져", "꼴통", "나대", "도련", "미친놈", "미친년", "염병할", "제기랄", "젠장",
			"옘병", "떼국", "왜놈", "오랑캐",

			// 한국어 욕설 확장
			"개좆", "씹새끼", "씹년", "썅년", "개년", "개자식", "존나", "졸라", "엿먹", "바보", "멍청이", "쓰레기", "찌질", "찐따", "호구", "고자", "걸뱅이",
			"구두쇠", "개떡", "개뿔", "개차반", "무뇌", "무개념", "오물", "딸딸이", "병신새끼", "빙신", "화냥년", "후장", "후빨", "간나새끼", "갈보", "노괴",
			"되놈", "똥개", "매춘부", "바보멍청이해삼멍게말미잘",

			// 줄임말 및 변형
			"ㅅㅂ", "ㅆㅂ", "ㅈㄴ", "ㅄ", "ㅂㅅ", "ㅁㅊ", "ㅗ", "ㄲㅈ", "개돼지", "한남충", "한녀충", "애비충", "맘충", "틀딱", "보빨", "보전깨", "보슬아치",
			"엠창", "엠생", "열폭");

	private final UserDao userDao;

	public UserAddValidator(UserDao userDao) {
		this.userDao = userDao;
	}

	public void validateForSignup(UserCreateRequest request) {
		boolean isSocial = request.getProvider() != null && !request.getProvider().isBlank()
				&& request.getProviderUserId() != null && !request.getProviderUserId().isBlank();

		if (!isSocial) {
			validatePasswordRule(request.getPassword());
			validatePasswordConfirm(request.getPassword(), request.getPasswordConfirm());
		}

		validateNicknameRule(request.getNickname());
		validateBadWord(request.getNickname());

		if (!isSocial) {
			validateEmailDuplicate(request.getUserId());
		}

		validateNicknameDuplicate(request.getNickname());

		if (!isSocial) {
			validatePhoneDuplicate(request.getPhone());
		}

		if (request.getCi() == null || request.getCi().isBlank()) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "본인인증이 필요합니다.");
		}
	}

	private void validatePasswordRule(String password) {
		if (password == null || !PASSWORD_PATTERN.matcher(password).matches()) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "비밀번호 형식이 올바르지 않습니다.");
		}
	}

	private void validatePasswordConfirm(String password, String passwordConfirm) {
		if (password == null || passwordConfirm == null || !password.equals(passwordConfirm)) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "비밀번호가 일치하지 않습니다.");
		}
	}

	private void validateNicknameRule(String nickname) {
		if (nickname == null || nickname.isBlank()) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "닉네임을 입력해 주세요.");
		}
		if (!nickname.matches("^[A-Za-z0-9가-힣]{2,10}$")) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "닉네임은 2~10자, 한글/영문/숫자만 가능합니다.");
		}
	}

	private void validateBadWord(String value) {
		if (value == null) {
			return;
		}
		String lower = value.toLowerCase();
		boolean hasBad = BAD_WORDS.stream().anyMatch(lower::contains);
		if (hasBad) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "닉네임에 사용할 수 없는 단어가 포함되어 있습니다.");
		}
	}

	private void validateEmailDuplicate(String userId) {
		if (userId == null || userId.isBlank()) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "아이디(이메일)를 입력해 주세요.");
		}
		if (userDao.existsByUserId(userId) > 0) {
			throw new BusinessException(ErrorCode.CONFLICT, "이미 사용중인 이메일입니다.");
		}
	}

	private void validateNicknameDuplicate(String nickname) {
		if (nickname == null || nickname.isBlank()) {
			return;
		}
		if (userDao.existsByNickname(nickname) > 0) {
			throw new BusinessException(ErrorCode.CONFLICT, "이미 사용중인 닉네임입니다.");
		}
	}

	private void validatePhoneDuplicate(String phone) {
		if (phone == null || phone.isBlank()) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "휴대폰 번호를 입력해 주세요.");
		}
		if (userDao.existsByPhone(phone) > 0) {
			throw new BusinessException(ErrorCode.DUPLICATED_PHONE);
		}
	}
}
