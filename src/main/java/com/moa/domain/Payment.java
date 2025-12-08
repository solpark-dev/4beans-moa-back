package com.moa.domain;

import java.time.LocalDateTime;

import com.moa.domain.enums.PaymentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 결제 도메인 클래스
 * PAYMENT 테이블과 1:1 매핑
 *
 * DB 스키마 기준:
 * - PAYMENT_ID (INT, PK, AUTO_INCREMENT)
 * - PARTY_ID (INT, FK → PARTY)
 * - PARTY_MEMBER_ID (INT, FK → PARTY_MEMBER)
 * - USER_ID (VARCHAR(50), FK → USERS)
 * - PAYMENT_TYPE (VARCHAR(20), DEFAULT 'MONTHLY')
 *   → MONTHLY: 월별 구독료
 *   → INITIAL: 첫 달 구독료 (파티원 가입 시)
 * - PAYMENT_AMOUNT (INT) - 결제 금액
 * - PAYMENT_STATUS (VARCHAR(20), DEFAULT 'PENDING')
 *   → PENDING: 결제 대기
 *   → COMPLETED: 결제 완료
 * - PAYMENT_METHOD (VARCHAR(20), DEFAULT 'CARD')
 * - PAYMENT_DATE (DATETIME) - 결제일시
 * - TOSS_PAYMENT_KEY (VARCHAR(255)) - Toss 결제 키
 * - ORDER_ID (VARCHAR(100)) - 주문 ID
 * - CARD_NUMBER (VARCHAR(20)) - 카드 번호 (마지막 4자리)
 * - CARD_COMPANY (VARCHAR(50)) - 카드사
 * - TARGET_MONTH (VARCHAR(7)) - 대상 월 (YYYY-MM)
 *
 * UNIQUE 제약:
 * - (PARTY_MEMBER_ID, TARGET_MONTH): 중복 결제 방지
 *
 * v1.0 비즈니스 로직:
 * - 방장: 다음 달부터 월 구독료 결제 (첫 달은 보증금만)
 * - 파티원: 가입 시 첫 달 구독료 즉시 결제
 * - 매월 파티 시작일에 자동 결제
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    private Integer paymentId;              // 결제 ID (PK)
    private Integer partyId;                // 파티 ID (FK)
    private Integer partyMemberId;          // 파티 멤버 ID (FK)
    private String userId;                  // 사용자 ID (FK)
    private String paymentType;             // 결제 타입 (MONTHLY, INITIAL)
    private Integer paymentAmount;          // 결제 금액
    private PaymentStatus paymentStatus;    // 결제 상태 (PENDING, COMPLETED)
    private String paymentMethod;           // 결제 수단 (CARD)
    private LocalDateTime paymentDate;      // 결제일시
    private String tossPaymentKey;          // Toss 결제 키
    private String orderId;                 // 주문 ID
    private String cardNumber;              // 카드 번호 (마지막 4자리)
    private String cardCompany;             // 카드사
    private String targetMonth;             // 대상 월 (YYYY-MM)
}