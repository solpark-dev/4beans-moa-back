package com.moa.dto.payment.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 결제 요청 DTO
 *
 * v1.0에서는:
 * 1. 방장 보증금 결제
 * 2. 파티원 통합 결제 (보증금 + 첫 달)
 *
 * 두 가지 결제 시나리오에서 모두 사용
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    /**
     * 결제 금액 (필수)
     */
    @NotNull(message = "결제 금액은 필수입니다.")
    private Integer amount;

    /**
     * 결제 수단 (필수, 기본: CARD)
     */
    @NotBlank(message = "결제 수단은 필수입니다.")
    private String paymentMethod;

    /**
     * Toss Payments에서 발급한 결제 키 (결제 승인 후)
     */
    private String tossPaymentKey;

    /**
     * 주문 ID (가맹점에서 생성)
     */
    private String orderId;
}