package com.moa.scheduler;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.moa.dao.deposit.DepositDao;
import com.moa.dao.refund.RefundRetryHistoryDao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * PENDING 상태 보증금 정리 스케줄러
 * 
 * 24시간 이상 PENDING 상태로 남아있는 Deposit 레코드를 정리합니다.
 * 이는 Toss 결제 실패 후 정리되지 않은 레코드를 처리합니다.
 * 
 * **Feature: transaction-compensation, Task 10.1**
 * **Validates: Requirements 6.2, 6.3**
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PendingDepositCleanupScheduler {

    private final DepositDao depositDao;
    private final RefundRetryHistoryDao refundRetryHistoryDao;

    // PENDING 상태 유지 최대 시간 (24시간)
    private static final int PENDING_TIMEOUT_HOURS = 24;

    /**
     * 매일 새벽 3시에 실행
     * 24시간 이상 PENDING 상태인 Deposit 삭제
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupStalePendingDeposits() {
        log.info("PENDING 상태 보증금 정리 스케줄러 시작");

        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusHours(PENDING_TIMEOUT_HOURS);
            
            // 24시간 이상 PENDING 상태인 Deposit 삭제
            int deletedCount = depositDao.deleteStalePendingRecords(cutoffTime);
            
            log.info("PENDING 상태 보증금 정리 완료: 삭제된 레코드 수={}, 기준시간={}", 
                    deletedCount, cutoffTime);

        } catch (Exception e) {
            log.error("PENDING 상태 보증금 정리 실패: {}", e.getMessage(), e);
        }
    }
}
