package com.moa.domain.enums;

/**
 * 파티 멤버 상태
 *
 * v1.0 상태 흐름:
 *
 * [방장]
 * PENDING_PAYMENT (파티 생성) → ACTIVE (보증금 결제 완료)
 *
 * [파티원]
 * PENDING_PAYMENT (가입 신청) → ACTIVE (보증금 + 첫달 결제 완료)
 *
 * PENDING_PAYMENT: 결제 대기 중
 * ACTIVE: 활성 상태 (결제 완료, 파티 참여 중)
 */
public enum MemberStatus {
	PENDING_PAYMENT("결제대기"),   // 보증금 결제 대기
	ACTIVE("활성"),                // 활성 상태 (결제 완료)
	INACTIVE("비활성");            // 탈퇴 상태

	private final String description;

	MemberStatus(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}
}