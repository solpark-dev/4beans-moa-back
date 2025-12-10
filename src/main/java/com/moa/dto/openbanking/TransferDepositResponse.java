package com.moa.dto.openbanking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 입금이체 응답 DTO
 * 오픈뱅킹 API 스펙 준수
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferDepositResponse {
    
    private String rspCode;          // 응답코드
    private String rspMessage;       // 응답메시지
    private String bankTranId;       // 거래고유번호
    private Integer tranAmt;         // 이체금액
    
    // 성공 응답 생성
    public static TransferDepositResponse success(String bankTranId, Integer tranAmt) {
        return TransferDepositResponse.builder()
                .rspCode("A0000")
                .rspMessage("이체 성공")
                .bankTranId(bankTranId)
                .tranAmt(tranAmt)
                .build();
    }
    
    // 에러 응답 생성
    public static TransferDepositResponse error(String rspCode, String rspMessage) {
        return TransferDepositResponse.builder()
                .rspCode(rspCode)
                .rspMessage(rspMessage)
                .build();
    }
}
