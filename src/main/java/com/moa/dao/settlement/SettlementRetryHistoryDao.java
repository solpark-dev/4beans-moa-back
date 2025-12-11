package com.moa.dao.settlement;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.moa.domain.SettlementRetryHistory;

/**
 * 정산 재시도 이력 DAO
 */
@Mapper
public interface SettlementRetryHistoryDao {

    /**
     * 재시도 이력 등록
     */
    int insertRetry(SettlementRetryHistory retry);

    /**
     * 재시도 이력 조회 (ID)
     */
    Optional<SettlementRetryHistory> findById(@Param("retryId") Integer retryId);

    /**
     * 특정 정산의 재시도 이력 조회
     */
    List<SettlementRetryHistory> findBySettlementId(@Param("settlementId") Integer settlementId);

    /**
     * 대기 중인 재시도 목록 조회 (PENDING 또는 FAILED 상태이고 nextRetryDate가 현재 시간 이전)
     */
    List<SettlementRetryHistory> findPendingRetries();

    /**
     * 재시도 상태 업데이트
     */
    int updateRetryStatus(SettlementRetryHistory retry);

    /**
     * 특정 정산의 최신 재시도 이력 조회
     */
    Optional<SettlementRetryHistory> findLatestBySettlementId(@Param("settlementId") Integer settlementId);

    /**
     * 특정 정산의 시도 횟수 조회
     */
    int countBySettlementId(@Param("settlementId") Integer settlementId);
}
