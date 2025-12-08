package com.moa.service.payment;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.moa.common.exception.BusinessException;
import com.moa.domain.Payment;
import com.moa.domain.PaymentRetryHistory;

/**
 * Payment Retry Service Interface
 * Manages payment retry attempts and exponential backoff scheduling
 *
 * Retry Strategy:
 * - Attempt 1: Initial payment attempt (immediate)
 * - Attempt 2: +24 hours after first failure
 * - Attempt 3: +48 hours after second failure
 * - Attempt 4: +72 hours after third failure (final attempt)
 *
 * @author MOA Team
 * @since 2025-12-04
 */
public interface PaymentRetryService {

    /**
     * Record successful payment attempt
     * Creates SUCCESS record in retry history
     *
     * @param payment Payment that succeeded
     * @param attemptNumber Attempt number (1-4)
     */
    void recordSuccess(Payment payment, int attemptNumber);

    /**
     * Record failed payment attempt with retry schedule
     * Creates FAILED record with NEXT_RETRY_DATE
     *
     * @param payment Payment that failed
     * @param attemptNumber Current attempt number (1-3)
     * @param errorCode Error code from payment gateway
     * @param errorMessage Error message from payment gateway
     * @param nextRetryDate When to retry next
     */
    void recordFailureWithRetry(
            Payment payment,
            int attemptNumber,
            String errorCode,
            String errorMessage,
            LocalDateTime nextRetryDate);

    /**
     * Record permanent payment failure
     * Creates FAILED record with no retry schedule (max attempts reached)
     *
     * @param payment Payment that permanently failed
     * @param attemptNumber Final attempt number (4)
     * @param exception Business exception with error details
     */
    void recordPermanentFailure(
            Payment payment,
            int attemptNumber,
            BusinessException exception);

    /**
     * Find all payments that need retry today
     * Queries retry history for FAILED status with NEXT_RETRY_DATE = today
     *
     * @param today Today's date
     * @return List of retry history records pending retry
     */
    List<PaymentRetryHistory> findPendingRetries(LocalDate today);

    /**
     * Retry a failed payment
     * Loads payment, verifies status, and calls PaymentService to attempt again
     *
     * @param retry Retry history record
     * @param targetMonth Target month for payment (YYYY-MM format)
     */
    void retryPayment(PaymentRetryHistory retry, String targetMonth);
}
