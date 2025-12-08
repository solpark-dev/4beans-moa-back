package com.moa.scheduler;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.moa.domain.RefundRetryHistory;
import com.moa.service.refund.RefundRetryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Refund Scheduler
 * Processes failed deposit refund retries automatically
 *
 * Schedule:
 * - Every hour: Check for pending refund retries and process them
 *
 * Retry Strategy:
 * - Attempt 1: Immediate (on initial refund failure)
 * - Attempt 2: +1 hour after first failure
 * - Attempt 3: +4 hours after second failure
 * - Attempt 4: +24 hours after third failure (final attempt)
 *
 * @author MOA Team
 * @since 2025-12-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefundScheduler {

    private final RefundRetryService refundRetryService;

    /**
     * Process pending refund retries
     * Runs every hour to check for failed refunds that need retry
     *
     * Cron: 0 0 * * * * = Every hour at :00 minutes
     */
    @Scheduled(cron = "0 0 * * * *")
    public void processRefundRetries() {
        log.info("===== Refund Retry Scheduler Started =====");

        try {
            // 1. Find all pending refund retries
            List<RefundRetryHistory> pendingRetries = refundRetryService.findPendingRetries();

            if (pendingRetries.isEmpty()) {
                log.info("No pending refund retries found");
                return;
            }

            log.info("Processing {} pending refund retries", pendingRetries.size());

            // 2. Process each retry
            int successCount = 0;
            int failureCount = 0;

            for (RefundRetryHistory retry : pendingRetries) {
                try {
                    refundRetryService.retryRefund(retry);
                    successCount++;
                } catch (Exception e) {
                    log.error("Failed to process refund retry: retryId={}, depositId={}, error={}",
                            retry.getRetryId(), retry.getDepositId(), e.getMessage(), e);
                    failureCount++;
                    // Continue with next retry (isolation)
                }
            }

            log.info("Refund retry processing completed: success={}, failure={}", successCount, failureCount);

        } catch (Exception e) {
            log.error("Refund retry scheduler failed", e);
        } finally {
            log.info("===== Refund Retry Scheduler Finished =====");
        }
    }
}
