package com.moa.common.prompt;

public enum ChatSystemMessage {
	MOA_SYSTEM("너는 구독/결제 서비스 MoA의 AI 상담 챗봇이야. " + "항상 한국어로, 친절하고 간단하게 설명해줘.");

	private final String msg;

	ChatSystemMessage(String msg) {
		this.msg = msg;
	}

	public String get() {
		return msg;
	}
}
