package com.moa.domain;

import java.time.LocalDateTime;

import com.moa.domain.enums.DepositStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 보증금 도메인 클래스
 * DEPOSIT 테이블과 1:1 매핑
 *
 * DB 스키마 기준:
 * - DEPOSIT_ID (INT, PK, AUTO_INCREMENT)
 * - PARTY_ID (INT, FK → PARTY)
 * - PARTY_MEMBER_ID (INT, FK → PARTY_MEMBER)
 * - USER_ID (VARCHAR(50), FK → USERS)
 * - DEPOSIT_TYPE (VARCHAR(20), DEFAULT 'SECURITY')
 * - DEPOSIT_AMOUNT (INT) - 보증금 금액
 * - DEPOSIT_STATUS (VARCHAR(20), DEFAULT 'PAID')
 *   → PAID: 결제 완료
 *   → REFUNDED: 환불 완료 (v2.0)
 *   → FORFEITED: 몰수 (v2.0, 악의적 탈퇴 시)
 * - PAYMENT_DATE (DATETIME) - 결제일시
 * - REFUND_DATE (DATETIME) - 환불일시
 * - REFUND_AMOUNT (INT) - 환불 금액
 * - TRANSACTION_DATE (DATETIME) - 트랜잭션 생성일시
 * - TOSS_PAYMENT_KEY (VARCHAR(255)) - Toss Payments 결제 키
 * - ORDER_ID (VARCHAR(100)) - 주문 ID
 *
 * v1.0 비즈니스 로직:
 * - 방장: 월구독료 전액 (예: Netflix 13,000원)
 * - 파티원: 인당 요금 (예: 13,000원 ÷ 4명 = 3,250원)
 * - v1.0에서는 환불 없음 (상태는 항상 PAID)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Deposit {

    private Integer depositId;              // 보증금 ID (PK)
    private Integer partyId;                // 파티 ID (FK)
    private Integer partyMemberId;          // 파티 멤버 ID (FK)
    private String userId;                  // 사용자 ID (FK)
    private String depositType;             // 보증금 타입 (기본: SECURITY)
    private Integer depositAmount;          // 보증금 금액
    private DepositStatus depositStatus;    // 보증금 상태 (PAID, REFUNDED, FORFEITED)
    private LocalDateTime paymentDate;      // 결제일시
    private LocalDateTime refundDate;       // 환불일시
    private Integer refundAmount;           // 환불 금액
    private LocalDateTime transactionDate;  // 트랜잭션 생성일시

    // Toss Payments 연동 필드
    private String tossPaymentKey;          // Toss 결제 키
    private String orderId;                 // 주문 ID
}