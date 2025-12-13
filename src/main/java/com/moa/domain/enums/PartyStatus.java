package com.moa.domain.enums;

/**
 * 파티 상태
 *
 * v1.0 상태 전이 흐름:
 * PENDING_PAYMENT → RECRUITING → ACTIVE → CLOSED
 *
 * PENDING_PAYMENT: 파티 생성됨, 방장 보증금 결제 대기
 * RECRUITING: 파티원 모집 중 (방장 보증금 결제 완료)
 * ACTIVE: 서비스 이용 중 (최대 인원 도달)
 * CLOSED: 파티 종료
 */
public enum PartyStatus {
	PENDING_PAYMENT("결제대기"),   // 방장 보증금 결제 대기
	RECRUITING("모집중"),          // 파티원 모집 중
	ACTIVE("이용중"),              // 서비스 이용 중 (인원 다 참)
	SUSPENDED("일시정지"),         // 결제 실패 등으로 일시정지
	DISBANDED("해산"),             // 파티 강제 해산 (파티장 탈퇴 등)
	CLOSED("종료");                // 파티 종료

	private final String description;

	PartyStatus(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}
}