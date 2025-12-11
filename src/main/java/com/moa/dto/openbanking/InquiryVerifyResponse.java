package com.moa.dto.openbanking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 인증코드 검증 응답 DTO
 * 오픈뱅킹 API 스펙 준수
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InquiryVerifyResponse {
    
    private String rspCode;          // 응답코드
    private String rspMessage;       // 응답메시지
    private boolean verified;        // 인증 성공 여부
    private String fintechUseNum;    // 핀테크이용번호 (인증 성공 시)
    
    // 인증 성공 응답
    public static InquiryVerifyResponse success(String fintechUseNum) {
        return InquiryVerifyResponse.builder()
                .rspCode("A0000")
                .rspMessage("인증 성공")
                .verified(true)
                .fintechUseNum(fintechUseNum)
                .build();
    }
    
    // 인증 실패 응답
    public static InquiryVerifyResponse fail(String rspCode, String rspMessage) {
        return InquiryVerifyResponse.builder()
                .rspCode(rspCode)
                .rspMessage(rspMessage)
                .verified(false)
                .build();
    }
}
