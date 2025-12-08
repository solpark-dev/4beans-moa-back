package com.moa.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payment Retry History domain class
 * Tracks all payment attempts (initial + retries) for monthly auto-payments
 *
 * Retry Strategy:
 * - Attempt 1: Initial payment attempt
 * - Attempt 2: +24h after failure
 * - Attempt 3: +48h after failure
 * - Attempt 4: +72h after failure (final attempt)
 *
 * @author MOA Team
 * @since 2025-12-04
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRetryHistory {

    /**
     * Retry history primary key
     */
    private Integer retryId;

    /**
     * Foreign key to PAYMENT table
     */
    private Integer paymentId;

    /**
     * Foreign key to PARTY table
     */
    private Integer partyId;

    /**
     * Foreign key to PARTY_MEMBER table
     */
    private Integer partyMemberId;

    /**
     * Attempt number (1-4)
     * 1 = initial attempt, 2-4 = retries
     */
    private Integer attemptNumber;

    /**
     * When this payment attempt was made
     */
    private LocalDateTime attemptDate;

    /**
     * Why this retry was needed (error message from previous attempt)
     */
    private String retryReason;

    /**
     * Status of this attempt: SUCCESS or FAILED
     */
    private String retryStatus;

    /**
     * When to retry next (null if status is SUCCESS)
     * Used by scheduler to find pending retries
     */
    private LocalDateTime nextRetryDate;

    /**
     * Toss Payments error code (if failed)
     */
    private String errorCode;

    /**
     * Toss Payments error message (if failed)
     */
    private String errorMessage;

    /**
     * Record creation timestamp
     */
    private LocalDateTime createdAt;
}
