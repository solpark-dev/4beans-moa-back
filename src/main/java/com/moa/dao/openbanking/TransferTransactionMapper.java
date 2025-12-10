package com.moa.dao.openbanking;

import com.moa.domain.openbanking.TransferTransaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 입금이체 거래 기록 Mapper
 */
@Mapper
public interface TransferTransactionMapper {
    
    // 거래 기록 저장
    void insert(TransferTransaction transaction);
    
    // 거래고유번호로 조회
    TransferTransaction findByBankTranId(@Param("bankTranId") String bankTranId);
    
    // 정산 ID로 거래 목록 조회
    List<TransferTransaction> findBySettlementId(@Param("settlementId") Integer settlementId);
    
    // 상태 업데이트
    void updateStatus(@Param("transactionId") Long transactionId, @Param("status") String status);
}
