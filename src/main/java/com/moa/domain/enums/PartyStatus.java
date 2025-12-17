package com.moa.domain.enums;

public enum PartyStatus {
	PENDING_PAYMENT("결제대기"),
	RECRUITING("모집중"),
	ACTIVE("이용중"),
	SUSPENDED("일시정지"),
	DISBANDED("해산"),
	CLOSED("종료");

	private final String description;

	PartyStatus(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}
}