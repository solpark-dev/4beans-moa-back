package com.moa.service.account;

import com.moa.dto.openbanking.*;
import com.moa.domain.Account;

/**
 * 계좌 등록 및 관리 서비스 인터페이스
 */
public interface BankAccountService {
    
    /**
     * 1원 인증 요청
     * @param userId 사용자 ID
     * @param bankCode 은행코드
     * @param accountNum 계좌번호
     * @param accountHolder 예금주명
     * @return 인증 응답 (거래고유번호, 인증코드 포함)
     */
    InquiryReceiveResponse requestVerification(String userId, String bankCode, String accountNum, String accountHolder);
    
    /**
     * 인증코드 검증 및 계좌 등록
     * @param userId 사용자 ID
     * @param bankTranId 거래고유번호
     * @param verifyCode 인증코드
     * @return 검증 응답 (핀테크이용번호 포함)
     */
    InquiryVerifyResponse verifyAndRegister(String userId, String bankTranId, String verifyCode);
    
    /**
     * 계좌 조회
     * @param userId 사용자 ID
     * @return 계좌 정보
     */
    Account getAccount(String userId);
    
    /**
     * 계좌 삭제 (비활성화)
     * @param userId 사용자 ID
     */
    void deleteAccount(String userId);
    
    /**
     * 계좌 변경 (기존 계좌 비활성화 후 새 인증 시작)
     * @param userId 사용자 ID
     * @param bankCode 새 은행코드
     * @param accountNum 새 계좌번호
     * @param accountHolder 새 예금주명
     * @return 인증 응답
     */
    InquiryReceiveResponse changeAccount(String userId, String bankCode, String accountNum, String accountHolder);
}
