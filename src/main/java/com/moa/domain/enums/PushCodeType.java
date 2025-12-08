package com.moa.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PushCodeType {
    
    // 커뮤니티 알림
    INQUIRY_ANSWER("INQUIRY_ANSWER", "COMMUNITY"),        // 문의 답변 완료
    
    // 결제 알림
    PAYMENT_SUCCESS("PAYMENT_SUCCESS", "PAYMENT"),        // 결제 성공
    PAYMENT_FAIL("PAYMENT_FAIL", "PAYMENT"),              // 결제 실패
    
    // 파티 알림
    PARTY_JOIN("PARTY_JOIN", "PARTY"),                    // 파티 가입 완료
    PARTY_START("PARTY_START", "PARTY"),                  // 파티 시작

    
    // 정산 알림
    SETTLEMENT_MONTHLY("SETTLEMENT_MONTHLY", "SETTLEMENT"), // 월간 정산 완료
    
    // 보증금 알림
    DEPOSIT_PAID("DEPOSIT_PAID", "DEPOSIT"),              // 보증금 납부 완료
    DEPOSIT_REFUND("DEPOSIT_REFUND", "DEPOSIT");          // 보증금 환불 완료
    
    private final String code;
    private final String moduleType;
}