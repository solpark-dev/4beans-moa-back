package com.moa.service.refund;

import java.time.LocalDateTime;
import java.util.List;

import com.moa.domain.RefundRetryHistory;

/**
 * Refund Retry Service Interface
 * Manages deposit refund retry attempts with exponential backoff scheduling
 *
 * Retry Strategy:
 * - Attempt 1: Initial refund attempt (immediate)
 * - Attempt 2: +1 hour after first failure
 * - Attempt 3: +4 hours after second failure
 * - Attempt 4: +24 hours after third failure (final attempt)
 *
 * @author MOA Team
 * @since 2025-12-06
 */
public interface RefundRetryService {

    /**
     * Find all refunds that need retry now
     * Queries retry history for FAILED/PENDING status with nextRetryDate <= now
     *
     * @return List of retry history records pending retry
     */
    List<RefundRetryHistory> findPendingRetries();

    /**
     * Retry a failed refund
     * Loads deposit, verifies status, and attempts refund again
     *
     * @param retry Retry history record
     */
    void retryRefund(RefundRetryHistory retry);

    /**
     * Calculate next retry date based on attempt number
     * Uses exponential backoff: 1h, 4h, 24h
     *
     * @param attemptNumber Current attempt number (1-3)
     * @return Next retry datetime
     */
    LocalDateTime calculateNextRetryDate(int attemptNumber);
}
