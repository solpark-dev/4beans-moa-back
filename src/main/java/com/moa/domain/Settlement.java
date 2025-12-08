package com.moa.domain;

import java.time.LocalDateTime;

import com.moa.domain.enums.SettlementStatus;

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
public class Settlement {
    private Integer settlementId;
    private Integer partyId;
    private String partyLeaderId;
    private Integer accountId;
    private String settlementMonth;
    private String settlementType;
    private Integer totalAmount;
    private Double commissionRate;
    private Integer commissionAmount;
    private Integer netAmount;
    private SettlementStatus settlementStatus;
    private LocalDateTime settlementDate;
    private String bankTranId; // 오픈뱅킹 거래고유번호
    private LocalDateTime regDate;
}
