package com.moa.common.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Monthly Payment Completed Event
 * Published when a monthly auto-payment successfully completes
 * Triggers notification to the user
 *
 * @author MOA Team
 * @since 2025-12-04
 */
@Getter
@RequiredArgsConstructor
public class MonthlyPaymentCompletedEvent {

    /**
     * Party ID where payment occurred
     */
    private final Integer partyId;

    /**
     * Party Member ID who made the payment
     */
    private final Integer partyMemberId;

    /**
     * User ID who made the payment
     */
    private final String userId;

    /**
     * Payment amount
     */
    private final Integer amount;

    /**
     * Target month for the payment (YYYY-MM format)
     */
    private final String targetMonth;
}
