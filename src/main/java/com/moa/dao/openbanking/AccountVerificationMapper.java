package com.moa.dao.openbanking;

import com.moa.domain.openbanking.AccountVerification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 1원 인증 세션 Mapper
 */
@Mapper
public interface AccountVerificationMapper {
    
    // 인증 세션 저장
    void insert(AccountVerification verification);
    
    // 거래고유번호로 조회
    AccountVerification findByBankTranId(@Param("bankTranId") String bankTranId);
    
    // 사용자 ID로 최근 인증 세션 조회
    AccountVerification findLatestByUserId(@Param("userId") String userId);
    
    // 상태 업데이트
    void updateStatus(@Param("verificationId") Long verificationId, @Param("status") String status);
    
    // 시도 횟수 증가
    void incrementAttemptCount(@Param("verificationId") Long verificationId);
    
    // 만료된 세션 일괄 업데이트
    int updateExpiredSessions();
}
