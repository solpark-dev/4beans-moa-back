package com.moa.service.openbanking;

import com.moa.dto.openbanking.*;

/**
 * 오픈뱅킹 클라이언트 인터페이스
 * Mock 구현체와 실제 구현체를 교체할 수 있도록 인터페이스로 정의
 */
public interface OpenBankingClient {
    
    /**
     * 1원 인증 요청 (수취조회)
     * @param request 인증 요청 정보
     * @return 인증 응답 (거래고유번호, 인증코드 포함)
     */
    InquiryReceiveResponse requestVerification(InquiryReceiveRequest request);
    
    /**
     * 인증코드 검증
     * @param request 검증 요청 정보
     * @return 검증 응답 (핀테크이용번호 포함)
     */
    InquiryVerifyResponse verifyCode(InquiryVerifyRequest request);
    
    /**
     * 입금이체
     * @param request 이체 요청 정보
     * @return 이체 응답 (거래고유번호 포함)
     */
    TransferDepositResponse transferDeposit(TransferDepositRequest request);
}
