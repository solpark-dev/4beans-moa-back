package com.moa.dto.payment.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 결제 응답 DTO
 * 결제 목록 조회 시 사용
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    // === 결제 기본 정보 ===
    private Integer paymentId;
    private Integer partyId;
    private String userId; // 추가: 정산 시 필요
    private Integer partyMemberId; // 추가: 정산 시 필요
    private String paymentType;
    private Integer paymentAmount;
    private String paymentStatus; // Enum → String 변환
    private String paymentMethod;
    private LocalDateTime paymentDate;
    private String targetMonth; // YYYY-MM

    // === 파티 정보 (JOIN) ===
    private String productName; // 상품명
    private String partyLeaderNickname; // 방장 닉네임

    // === 카드 정보 ===
    private String cardNumber; // 카드 번호 (마스킹 처리된)
    private String cardCompany; // 카드사

    // === 재시도 정보 (JOIN with PAYMENT_RETRY_HISTORY) ===
    private String retryStatus; // 최신 재시도 상태: SUCCESS, FAILED, null
    private Integer attemptNumber; // 현재 시도 횟수 (1-4)
    private LocalDateTime nextRetryDate; // 다음 재시도 예정일
    private String retryReason; // 결제 실패 사유
    private String errorMessage; // 상세 에러 메시지
}