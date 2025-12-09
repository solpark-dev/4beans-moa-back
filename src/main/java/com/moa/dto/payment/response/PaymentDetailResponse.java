package com.moa.dto.payment.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 결제 상세 응답 DTO
 * 결제 상세 내역 조회 시 사용
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDetailResponse {

    // === 결제 기본 정보 ===
    private Integer paymentId;
    private Integer partyId;
    private Integer partyMemberId;
    private String userId;
    private String paymentType;
    private Integer paymentAmount;
    private String paymentStatus;
    private String paymentMethod;
    private LocalDateTime paymentDate;
    private String targetMonth;

    // === Toss Payments 정보 ===
    private String tossPaymentKey;
    private String orderId;
    private String cardNumber;
    private String cardCompany;

    // === 파티 정보 (JOIN) ===
    private String productName;
    private String productImage;
    private String partyLeaderNickname;

    // === 사용자 정보 (JOIN) ===
    private String userNickname;
}