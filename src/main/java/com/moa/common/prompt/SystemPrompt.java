package com.moa.common.prompt;

public enum SystemPrompt {

	MOA_CHATBOT("너는 구독/결제 서비스 MoA의 공식 상담 챗봇이야. " + "친절하고 부드러운 반말로 핵심 위주로 설명해.");

	private final String content;

	SystemPrompt(String content) {
		this.content = content;
	}

	public String content() {
		return content;
	}
}
