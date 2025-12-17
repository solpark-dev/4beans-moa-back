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
public class SettlementRetryHistory {

    private Integer retryId;
    private Integer settlementId;
    private Integer attemptNumber;
    private LocalDateTime attemptDate;
    private String retryReason;
    private String retryStatus;
    private LocalDateTime nextRetryDate;
    private Integer transferAmount;
    private String errorCode;
    private String errorMessage;
    private String bankRspCode;
    private String bankRspMessage;
    private String bankTranId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
}
