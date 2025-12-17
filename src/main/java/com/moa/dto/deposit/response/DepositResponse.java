package com.moa.dto.deposit.response;

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
public class DepositResponse {

    private Integer depositId;
    private Integer partyId;
    private Integer partyMemberId;
    private String userId;
    private String depositType;
    private Integer depositAmount;
    private String depositStatus;
    private LocalDateTime paymentDate;
    private LocalDateTime refundDate;
    private Integer refundAmount;
    private String tossPaymentKey;
    private String orderId;
    private String partyLeaderNickname; 
    private String productName; 
    private String userNickname; 
}