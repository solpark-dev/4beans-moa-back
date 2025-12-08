package com.moa.common.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Monthly Payment Failed Event
 * Published when a monthly auto-payment permanently fails (max retries exceeded)
 * Triggers notification to the user to update payment method
 *
 * @author MOA Team
 * @since 2025-12-04
 */
@Getter
@RequiredArgsConstructor
public class MonthlyPaymentFailedEvent {

    /**
     * Party ID where payment failed
     */
    private final Integer partyId;

    /**
     * Party Member ID whose payment failed
     */
    private final Integer partyMemberId;

    /**
     * User ID whose payment failed
     */
    private final String userId;

    /**
     * Target month for the payment (YYYY-MM format)
     */
    private final String targetMonth;

    /**
     * Error message describing why the payment failed
     */
    private final String errorMessage;
}
