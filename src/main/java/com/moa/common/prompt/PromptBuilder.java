package com.moa.common.prompt;

public class PromptBuilder {

	public static String buildForChat(String question, String context) {

		if (context == null || context.isBlank()) {
			context = "관련된 지식이 부족해. 일반적인 규칙에 맞게 답변해줘.";
		}

		return """
				아래는 MoA 내부 지식베이스야. 이 내용을 참고해서 답변해줘.

				[지식베이스]
				%s

				[사용자 질문]
				%s

				답변은 3~5문장 안에서 간결하고 친절하게 부탁해.
				""".formatted(context, question);
	}
}
