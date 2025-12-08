package com.moa.dao.payment;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.moa.domain.PaymentRetryHistory;

/**
 * Payment Retry History DAO
 * Manages payment retry attempts and history
 *
 * @author MOA Team
 * @since 2025-12-04
 */
@Mapper
public interface PaymentRetryDao {

    /**
     * Insert new retry history record
     *
     * @param history PaymentRetryHistory to insert
     * @return Number of rows inserted
     */
    int insert(PaymentRetryHistory history);

    /**
     * Find retry history by retry ID
     *
     * @param retryId Retry ID
     * @return Optional containing PaymentRetryHistory if found
     */
    Optional<PaymentRetryHistory> findById(@Param("retryId") Integer retryId);

    /**
     * Find all retry history for a payment
     *
     * @param paymentId Payment ID
     * @return List of all retry attempts for this payment
     */
    List<PaymentRetryHistory> findByPaymentId(@Param("paymentId") Integer paymentId);

    /**
     * Find payments that need retry today
     * Returns FAILED retries where NEXT_RETRY_DATE is today
     *
     * @param today Today's date
     * @return List of retry history records pending retry
     */
    List<PaymentRetryHistory> findByNextRetryDate(@Param("today") LocalDate today);

    /**
     * Get latest retry history for a payment
     * Ordered by attempt number descending
     *
     * @param paymentId Payment ID
     * @return Optional containing latest retry history
     */
    Optional<PaymentRetryHistory> findLatestByPaymentId(@Param("paymentId") Integer paymentId);
}
