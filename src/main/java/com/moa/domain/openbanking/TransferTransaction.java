package com.moa.domain.openbanking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 입금이체 거래 기록 도메인
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferTransaction {

    private Long transactionId;
    private Integer settlementId;
    private String bankTranId;
    private String fintechUseNum;
    private Integer tranAmt;
    private String printContent;
    private String reqClientName;
    private String rspCode;
    private String rspMessage;
    private TransactionStatus status; // PENDING, SUCCESS, FAILED
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
