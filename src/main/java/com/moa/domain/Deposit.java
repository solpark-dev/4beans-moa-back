package com.moa.domain;

import java.time.LocalDateTime;

import com.moa.domain.enums.DepositStatus;

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
public class Deposit {

    private Integer depositId;
    private Integer partyId;
    private Integer partyMemberId;
    private String userId;
    private String depositType;
    private Integer depositAmount;
    private DepositStatus depositStatus;
    private LocalDateTime paymentDate;
    private LocalDateTime refundDate;
    private Integer refundAmount;
    private LocalDateTime transactionDate;

    private String tossPaymentKey;
    private String orderId;
}