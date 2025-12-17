package com.moa.domain;

import java.time.LocalDateTime;

import com.moa.domain.enums.PaymentStatus;

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
public class Payment {

    private Integer paymentId;
    private Integer partyId;
    private Integer partyMemberId;
    private String userId;
    private String paymentType;
    private Integer paymentAmount;
    private PaymentStatus paymentStatus;
    private String paymentMethod;
    private LocalDateTime paymentDate;
    private String tossPaymentKey;
    private String orderId;
    private String cardNumber;
    private String cardCompany;
    private String targetMonth;
    private Integer settlementId;
}