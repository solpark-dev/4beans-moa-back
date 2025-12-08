package com.moa.common.prompt;

public class ChatPromptTemplate {

	public static String build(String question, String context) {
		if (context == null || context.isBlank()) {
			context = "관련 지식을 찾지 못했어. 서비스 규칙과 일반적인 상식에 기반해 조심스럽게 답변해줘.";
		}

		return """
				너는 구독·결제·파티·계정·정산을 도와주는 MoA 고객센터 챗봇이야.

				아래는 MoA 지식베이스에서 검색된 내용이야. 신뢰할 수 있는 정보라면 이를 최우선으로 사용해.
				[지식베이스]
				%s

				[사용자 질문]
				%s

				답변 규칙:
				- 한국어로 3~5문장 안에서 간결하게 답변해.
				- 서비스 내 메뉴 경로가 있으면 '마이페이지 > 내 정보'처럼 경로를 함께 알려줘.
				- 확실하지 않은 내용은 추측하지 말고 모른다고 말한 뒤, 사용자가 어디서 더 확인할 수 있을지 안내해.
				""".formatted(context, question);
	}
}
