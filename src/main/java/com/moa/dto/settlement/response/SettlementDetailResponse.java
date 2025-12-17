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
public class SettlementDetailResponse {
    private Integer detailId;
    private Integer settlementId;
    private Integer paymentId;
    private String userId;
    private String userNickname;
    private Integer paymentAmount;
    private LocalDateTime regDate;
}
