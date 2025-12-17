package com.moa.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRetryHistory {

    private Integer retryId;

    private Integer depositId;

    private String tossPaymentKey;

    private Integer attemptNumber;

    private LocalDateTime attemptDate;

    private String retryStatus;

    private LocalDateTime nextRetryDate;

    private String errorCode;

    private String errorMessage;

    private Integer refundAmount;

    private String refundReason;

    private String retryType;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
