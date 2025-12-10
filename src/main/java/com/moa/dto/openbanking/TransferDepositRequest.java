package com.moa.dto.openbanking;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 입금이체 요청 DTO
 * 오픈뱅킹 API 스펙 준수
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferDepositRequest {
    
    @NotBlank(message = "핀테크이용번호는 필수입니다")
    private String fintechUseNum;    // 핀테크이용번호
    
    @NotNull(message = "이체금액은 필수입니다")
    @Min(value = 1, message = "이체금액은 1원 이상이어야 합니다")
    private Integer tranAmt;         // 이체금액
    
    private String printContent;     // 입금통장 인자내역 (예: "MOA정산")
    
    private String reqClientName;    // 요청고객성명 (예: "MOA")
}
