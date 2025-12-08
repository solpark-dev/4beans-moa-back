package com.moa.dto.deposit.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 보증금 응답 DTO
 * 보증금 내역 조회 시 사용
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepositResponse {

    // === 보증금 기본 정보 ===
    private Integer depositId;
    private Integer partyId;
    private Integer partyMemberId;
    private String userId;
    private String depositType;
    private Integer depositAmount;
    private String depositStatus;           // Enum → String 변환
    private LocalDateTime paymentDate;
    private LocalDateTime refundDate;
    private Integer refundAmount;

    // === Toss Payments 정보 ===
    private String tossPaymentKey;
    private String orderId;

    // === 파티 정보 (JOIN) ===
    private String partyLeaderNickname;     // 방장 닉네임
    private String productName;             // 상품명

    // === 사용자 정보 (JOIN) ===
    private String userNickname;            // 결제자 닉네임
}