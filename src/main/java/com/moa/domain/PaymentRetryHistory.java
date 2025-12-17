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
public class PaymentRetryHistory {

    private Integer retryId;

    private Integer paymentId;

    private Integer partyId;

    private Integer partyMemberId;

    private Integer attemptNumber;

    private LocalDateTime attemptDate;

    private String retryReason;

    private String retryStatus;

    private LocalDateTime nextRetryDate;

    private String errorCode;

    private String errorMessage;

    private LocalDateTime createdAt;
}
