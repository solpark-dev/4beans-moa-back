package com.moa.domain.enums;

public enum DepositStatus {
	PENDING("결제대기"), PAID("결제완료"), REFUNDED("환불완료"), FORFEITED("몰수");

	private final String description;

	DepositStatus(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}
}