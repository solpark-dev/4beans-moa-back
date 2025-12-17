package com.moa.scheduler;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.moa.dao.deposit.DepositDao;
import com.moa.dao.refund.RefundRetryHistoryDao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PendingDepositCleanupScheduler {

	private final DepositDao depositDao;
	private final RefundRetryHistoryDao refundRetryHistoryDao;
	private static final int PENDING_TIMEOUT_HOURS = 24;

	@Scheduled(cron = "0 0 3 * * *")
	@Transactional
	public void cleanupStalePendingDeposits() {
		log.info("PENDING 상태 보증금 정리 스케줄러 시작");

		try {
			LocalDateTime cutoffTime = LocalDateTime.now().minusHours(PENDING_TIMEOUT_HOURS);

			int deletedCount = depositDao.deleteStalePendingRecords(cutoffTime);

			log.info("PENDING 상태 보증금 정리 완료: 삭제된 레코드 수={}, 기준시간={}", deletedCount, cutoffTime);

		} catch (Exception e) {
			log.error("PENDING 상태 보증금 정리 실패: {}", e.getMessage(), e);
		}
	}
}
