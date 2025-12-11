package com.moa.dto.openbanking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 인증코드 검증 요청 DTO
 * 오픈뱅킹 API 스펙 준수
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InquiryVerifyRequest {
    
    @NotBlank(message = "거래고유번호는 필수입니다")
    private String bankTranId;       // 거래고유번호
    
    @NotBlank(message = "인증코드는 필수입니다")
    @Size(min = 4, max = 4, message = "인증코드는 4자리입니다")
    private String verifyCode;       // 사용자 입력 인증코드
}
