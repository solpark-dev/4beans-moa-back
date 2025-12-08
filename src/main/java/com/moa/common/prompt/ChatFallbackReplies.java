package com.moa.common.prompt;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class ChatFallbackReplies {

	private static final List<String> DEFAULT_REPLIES = List.of("관련 정보를 찾지 못했어. 조금 더 구체적으로 물어봐 줄래?",
			"아직 등록된 답변이 없는 내용이라서 정확히 안내하기 어려워.", "지금은 정확한 정보를 찾지 못했어. 고객센터로 문의하면 더 빨리 도와줄 수 있어.",
			"조금 다르게 표현해서 한 번만 더 질문해줘.", "핵심 키워드만 두세 개로 나눠서 다시 물어봐 주면 더 잘 찾을 수 있어.",
			"내가 알고 있는 범위를 벗어난 것 같아. 메뉴 이름이나 화면 위치를 같이 알려줄래?");

	private static final Map<String, List<String>> CATEGORY_REPLIES = Map.of("구독", List.of(
			"구독 관련해서 딱 맞는 답변을 찾지 못했어. 사용 중인 OTT 이름이나 상황을 조금 더 알려줄래?", "구독 상품마다 조건이 달라서 정확한 정보를 찾기 어려워. 어떤 구독인지 함께 알려줘.",
			"무료체험인지, 정기 구독인지에 따라 안내가 달라져. 현재 이용 중인 구독 종류를 알려줄래?", "신규 구독인지 해지/변경인지 헷갈려. 하고 싶은 작업이 뭔지 한 번만 더 적어줄래?"),
			"결제",
			List.of("결제 관련해서 등록된 답변이 없어서 정확히 안내하기 어려워. 결제 수단과 오류 메시지를 알려줄래?",
					"결제 내역을 확인해야 하는 상황 같아. 결제일자나 사용한 카드 정보를 조금 더 알려줘.",
					"카드 결제인지 간편결제인지에 따라 처리 방법이 달라. 어떤 방식으로 결제했는지 말해줄래?",
					"동일 카드로 여러 번 시도했다면 승인 상태를 먼저 확인하는 게 좋아. 언제, 얼마를 결제했는지도 알려줘."),
			"계정",
			List.of("계정/회원정보 쪽에는 아직 등록된 답변이 부족해. 로그인 방식이나 오류 메시지를 같이 알려줄래?",
					"어떤 계정 문제인지 정확히 모르겠어. 로그인/비밀번호/닉네임 중 무엇이 문제야?",
					"내정보 수정, 소셜 연동, 탈퇴 중에 어떤 작업을 하려는 건지 알려주면 더 정확히 안내할 수 있어.",
					"사용한 로그인 수단(이메일, 카카오, 구글)을 알려주면 계정 문제를 더 잘 찾을 수 있어."),
			"파티",
			List.of("파티 관련해서 딱 맞는 지식을 찾지 못했어. 파티 만들기인지, 파티 참여인지 알려줘.", "어떤 파티 기능에서 막혔는지 더 자세히 말해주면 도와줄 수 있어.",
					"파티장인지 파티원인지에 따라 권한이 달라. 지금 어떤 역할인지 같이 적어줄래?",
					"새 파티를 만들고 싶은지, 이미 있는 파티에 들어가고 싶은지 알려주면 더 정확히 답해줄 수 있어."),
			"정산",
			List.of("정산 쪽 지식에서 일치하는 내용을 찾지 못했어. 정산일·금액·지갑 중 어떤 게 궁금한지 알려줄래?",
					"정산 내역 확인이 필요한 것 같아. 기간이나 금액을 같이 말해주면 더 정확히 안내할 수 있어.",
					"수익 분배, 출금, 수수료 중 무엇이 궁금한지 알려주면 관련 정보를 더 잘 찾을 수 있어.", "정산 주기(매일/매주/매월)와 연결된 계정을 알려주면 안내가 더 쉬워져."),
			"오류",
			List.of("오류 상황이지만 등록된 해결 방법을 찾지 못했어. 화면에 나온 오류 메시지를 그대로 알려줄래?",
					"정확한 오류 정보를 찾지 못했어. 어떤 화면에서 어떤 동작을 했는지 조금 더 설명해줘.",
					"앱/웹, 사용 중인 브라우저, 기기 정보를 알려주면 오류 원인을 더 좁힐 수 있어.",
					"오류가 계속 반복된다면 발생 시각과 함께 캡처를 남겨주는 게 좋아. 그런 정보가 있으면 말해줄래?"),
			"기타",
			List.of("아직 이 주제에 대한 지식이 많이 쌓이지 않았어. 궁금한 내용을 조금 더 자세히 써줄래?", "정확히 어떤 기능을 말하는지 모르겠어. 메뉴 이름이나 화면 위치를 같이 알려줘.",
					"MoA 서비스 안에서의 이야기인지, 일반적인 질문인지 먼저 알려주면 도움이 돼.", "가능하면 한 번에 한 가지 주제만 질문해줘. 그러면 더 정확하게 답해줄 수 있어."));

	public static String fallback(String category) {
		if (category == null || category.isBlank()) {
			return random(DEFAULT_REPLIES);
		}
		List<String> list = CATEGORY_REPLIES.get(category);
		if (list == null || list.isEmpty()) {
			return random(DEFAULT_REPLIES);
		}
		return random(list);
	}

	private static String random(List<String> list) {
		if (list == null || list.isEmpty()) {
			return "관련 정보를 찾지 못했어.";
		}
		int index = ThreadLocalRandom.current().nextInt(list.size());
		return list.get(index);
	}

	private ChatFallbackReplies() {
	}
}
