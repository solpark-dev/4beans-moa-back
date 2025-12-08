package com.moa.dto.settlement.response;

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
public class SettlementResponse {
    private Integer settlementId;
    private Integer partyId;
    private String partyLeaderId;
    private String settlementMonth;
    private Integer totalAmount;
    private Integer netAmount;
    private String settlementStatus;
    private LocalDateTime settlementDate;
}
