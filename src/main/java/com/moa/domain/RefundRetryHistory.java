package com.moa.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 환불 재시도 이력 엔티티
 * 
 * 보증금 환불 실패 시 재시도 추적용
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRetryHistory {

    private Integer retryId;

    /** 보증금 ID (FK) */
    private Integer depositId;

    /** Toss 결제 키 (보상 트랜잭션용) */
    private String tossPaymentKey;

    /** 재시도 횟수 */
    private Integer attemptNumber;

    /** 시도 일시 */
    private LocalDateTime attemptDate;

    /** 재시도 상태 (PENDING, IN_PROGRESS, FAILED, SUCCESS) */
    private String retryStatus;

    /** 다음 재시도 예정 일시 */
    private LocalDateTime nextRetryDate;

    /** 에러 코드 */
    private String errorCode;

    /** 에러 메시지 */
    private String errorMessage;

    /** 환불 금액 */
    private Integer refundAmount;

    /** 환불 사유 */
    private String refundReason;

    /** 재시도 유형 (REFUND: 환불 재시도, COMPENSATION: 보상 트랜잭션) */
    private String retryType;

    /** 생성일시 */
    private LocalDateTime createdAt;

    /** 수정일시 */
    private LocalDateTime updatedAt;
}
