package com.moa.domain.enums;

/**
 * 결제 상태
 *
 * v1.0: 모든 결제는 즉시 COMPLETED (Happy Path)
 * v2.0: PENDING → 재시도 → FAILED 흐름 추가 예정
 *
 * PENDING: 결제 대기 중
 * COMPLETED: 결제 완료
 * FAILED: 결제 실패 (v2.0)
 */
public enum PaymentStatus {
    PENDING("결제대기"),
    COMPLETED("결제완료"),
    FAILED("결제실패"),
    REFUNDED("결제환불");

    private final String description;

    PaymentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}