package com.moa.domain.enums;

public enum MemberStatus {
	PENDING_PAYMENT("결제대기"), ACTIVE("활성"), INACTIVE("비활성");

	private final String description;

	MemberStatus(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}
}