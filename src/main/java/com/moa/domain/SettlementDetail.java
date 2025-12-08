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
public class SettlementDetail {
    private Integer detailId;
    private Integer settlementId;
    private Integer paymentId;
    private Integer partyMemberId;
    private String userId;
    private Integer paymentAmount;
    private LocalDateTime regDate;
}
