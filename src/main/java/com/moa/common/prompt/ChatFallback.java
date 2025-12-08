package com.moa.common.prompt;

public enum ChatFallback {

	HELLO("안녕! 나는 MoA 챗봇이야. 무엇을 도와줄까?"), SUBSCRIPTION("구독 정보는 마이페이지 > 내 구독에서 확인할 수 있어!"),
	PAYMENT("결제 내역은 마이페이지 > 결제 관리에서 볼 수 있어!"), WITHDRAW("회원탈퇴는 마이페이지 > 계정 설정에서 진행할 수 있어!"),
	GENERAL("조금 더 구체적으로 말해줄래? 구독, 결제, 회원 관련이면 도와줄 수 있어!");

	private final String reply;

	ChatFallback(String reply) {
		this.reply = reply;
	}

	public String reply() {
		return reply;
	}
}
