package com.moa.domain.enums;

/**
 * 보증금 상태
 *
 * v1.0: PAID만 사용
 * v2.0: REFUNDED, FORFEITED 추가 예정
 *
 * PAID: 결제 완료
 * REFUNDED: 환불 완료 (정상 탈퇴 시)
 * FORFEITED: 몰수 (악의적 탈퇴 시, 정산으로 방장에게 지급)
 */
public enum DepositStatus {
    PAID("결제완료"),
    REFUNDED("환불완료"),
    FORFEITED("몰수");

    private final String description;

    DepositStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}