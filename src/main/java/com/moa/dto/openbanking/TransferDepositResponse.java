package com.moa.dto.openbanking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferDepositResponse {
    
    private String rspCode;
    private String rspMessage;
    private String bankTranId;
    private Integer tranAmt;

    public static TransferDepositResponse success(String bankTranId, Integer tranAmt) {
        return TransferDepositResponse.builder()
                .rspCode("A0000")
                .rspMessage("이체 성공")
                .bankTranId(bankTranId)
                .tranAmt(tranAmt)
                .build();
    }

    public static TransferDepositResponse error(String rspCode, String rspMessage) {
        return TransferDepositResponse.builder()
                .rspCode(rspCode)
                .rspMessage(rspMessage)
                .build();
    }
}
