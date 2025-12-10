package com.moa.dto.openbanking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 1원 인증 요청 DTO (수취조회)
 * 오픈뱅킹 API 스펙 준수
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InquiryReceiveRequest {
    
    @NotBlank(message = "은행코드는 필수입니다")
    @Size(min = 3, max = 3, message = "은행코드는 3자리입니다")
    private String bankCodeStd;      // 은행코드 (예: 004)
    
    @NotBlank(message = "계좌번호는 필수입니다")
    @Size(max = 20, message = "계좌번호는 20자 이내입니다")
    private String accountNum;       // 계좌번호
    
    @NotBlank(message = "예금주명은 필수입니다")
    @Size(max = 50, message = "예금주명은 50자 이내입니다")
    private String accountHolderInfo; // 예금주명
    
    @Builder.Default
    private String tranAmt = "1";    // 이체금액 (1원 고정)
    
    private String printContent;     // 입금통장 인자내역 (인증코드)
}
