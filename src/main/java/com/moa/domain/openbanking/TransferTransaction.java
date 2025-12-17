package com.moa.domain.openbanking;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private TransactionStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
