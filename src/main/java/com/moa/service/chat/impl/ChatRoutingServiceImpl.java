package com.moa.service.chat.impl;

import org.springframework.stereotype.Service;

import com.moa.domain.ChatRoute;
import com.moa.service.chat.ChatRoutingService;

@Service
public class ChatRoutingServiceImpl implements ChatRoutingService {

	@Override
	public ChatRoute route(String text) {
		if (text == null || text.isBlank()) {
			return new ChatRoute("기타", "");
		}

		String normalized = text.replaceAll("\\s+", "");

		if (contains(normalized, "구독", "상품", "OTT", "넷플릭스", "티빙", "웨이브", "디즈니", "요금제", "플랜", "멤버십")) {
			return new ChatRoute("구독", "구독");
		}

		if (contains(normalized, "결제", "카드", "청구", "요금", "환불", "결제수단", "자동결제", "무통장", "입금", "쿠폰", "포인트", "결제내역")) {
			return new ChatRoute("결제", "결제");
		}

		if (contains(normalized, "회원", "계정", "로그인", "비밀번호", "탈퇴", "닉네임", "프로필", "휴대폰", "휴대전화", "아이디", "이메일", "내정보",
				"정보수정", "회원정보", "마이페이지", "연동", "소셜로그인", "카카오", "구글")) {
			return new ChatRoute("계정", "계정");
		}

		if (contains(normalized, "파티", "모집", "참여", "참가", "파티장", "파티원", "구독공유", "호스트", "방장", "초대")) {
			return new ChatRoute("파티", "파티");
		}

		if (contains(normalized, "정산", "수익", "지급", "출금", "정산일", "지갑", "잔액", "분배", "수수료", "정기정산")) {
			return new ChatRoute("정산", "정산");
		}

		if (contains(normalized, "오류", "에러", "안열려", "안돼", "로딩", "버그", "튕김", "멈춤", "흰화면", "버튼안먹", "팝업안뜸")) {
			return new ChatRoute("오류", "오류");
		}

		return new ChatRoute("기타", text);
	}

	private boolean contains(String text, String... keywords) {
		for (String keyword : keywords) {
			if (text.contains(keyword)) {
				return true;
			}
		}
		return false;
	}
}
