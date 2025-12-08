package com.moa.dao.refund;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.moa.domain.RefundRetryHistory;

/**
 * 환불 재시도 이력 DAO
 */
@Mapper
public interface RefundRetryHistoryDao {

    /**
     * 환불 재시도 이력 삽입
     */
    int insertRefundRetry(RefundRetryHistory refundRetry);

    /**
     * ID로 조회
     */
    Optional<RefundRetryHistory> findById(@Param("retryId") Integer retryId);

    /**
     * 보증금 ID로 조회
     */
    List<RefundRetryHistory> findByDepositId(@Param("depositId") Integer depositId);

    /**
     * 재시도 대기 중인 목록 조회 (PENDING/FAILED 상태)
     */
    List<RefundRetryHistory> findPendingRetries();

    /**
     * 재시도 상태 업데이트
     */
    int updateRetryStatus(RefundRetryHistory refundRetry);

    /**
     * 보증금 ID로 마지막 재시도 이력 조회
     */
    Optional<RefundRetryHistory> findLatestByDepositId(@Param("depositId") Integer depositId);
}
