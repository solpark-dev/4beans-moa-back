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

    /**
     * Record refund failure with REQUIRES_NEW transaction
     * Ensures failure history is preserved even if parent transaction rolls back
     *
     * @param deposit Deposit that failed to refund
     * @param e Exception that occurred
     * @param reason Refund reason
     */
    void recordFailure(com.moa.domain.Deposit deposit, Exception e, String reason);

    /**
     * Register compensation transaction for Toss payment cancellation
     * Used when Toss payment succeeded but DB save failed
     *
     * @param depositId Deposit ID
     * @param tossPaymentKey Toss payment key for cancellation
     * @param amount Amount to cancel
     * @param reason Reason for compensation
     */
    void registerCompensation(Integer depositId, String tossPaymentKey, Integer amount, String reason);

    /**
     * Record compensation transaction needed (REQUIRES_NEW transaction)
     * Used when Toss payment succeeded but DB update failed
     *
     * @param deposit Deposit in PENDING state
     * @param reason Reason for compensation
     */
    void recordCompensation(com.moa.domain.Deposit deposit, String reason);
}
