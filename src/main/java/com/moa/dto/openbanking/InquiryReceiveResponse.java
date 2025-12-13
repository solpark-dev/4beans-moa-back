package com.moa.dto.openbanking;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 1원 인증 응답 DTO (수취조회)
 * 오픈뱅킹 API 스펙 준수
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InquiryReceiveResponse {

    private String rspCode;           // 응답코드 (A0000: 성공)
    private String rspMessage;        // 응답메시지
    private String bankTranId;        // 거래고유번호
    private String printContent;      // 인증코드 (입금통장 인자내역)
    private String maskedAccount;     // 마스킹된 계좌번호
    private LocalDateTime expiresAt;  // 인증 만료 시간

    // 성공 응답 생성
    public static InquiryReceiveResponse success(String bankTranId, String printContent) {
        return InquiryReceiveResponse.builder()
                .rspCode("A0000")
                .rspMessage("성공")
                .bankTranId(bankTranId)
                .printContent(printContent)
                .build();
    }

    // 성공 응답 생성 (전체 정보 포함)
    public static InquiryReceiveResponse success(String bankTranId, String printContent, String maskedAccount, LocalDateTime expiresAt) {
        return InquiryReceiveResponse.builder()
                .rspCode("A0000")
                .rspMessage("성공")
                .bankTranId(bankTranId)
                .printContent(printContent)
                .maskedAccount(maskedAccount)
                .expiresAt(expiresAt)
                .build();
    }

    // 에러 응답 생성
    public static InquiryReceiveResponse error(String rspCode, String rspMessage) {
        return InquiryReceiveResponse.builder()
                .rspCode(rspCode)
                .rspMessage(rspMessage)
                .build();
    }
}
