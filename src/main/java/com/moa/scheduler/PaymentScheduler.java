package com.moa.scheduler;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.moa.dao.party.PartyDao;
import com.moa.dao.partymember.PartyMemberDao;
import com.moa.domain.Party;
import com.moa.domain.PartyMember;
import com.moa.domain.PaymentRetryHistory;
import com.moa.service.payment.PaymentRetryService;
import com.moa.service.payment.PaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Payment Scheduler
 * Daily scheduler for processing monthly auto-payments with retry logic
 *
 * Handles:
 * 1. New monthly payments for parties whose payment day is today
 * 2. Retry attempts for previously failed payments
 *
 * @author MOA Team
 * @since 2025-12-04
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentScheduler {

    private final PartyDao partyDao;
    private final PartyMemberDao partyMemberDao;
    private final PaymentService paymentService;
    private final PaymentRetryService retryService;

    /**
     * Daily payment scheduler - runs at 2:00 AM
     * Processes both new monthly payments and pending retries
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void runDailyPayments() {
        log.info("Starting daily payment scheduler...");

        LocalDate today = LocalDate.now();
        String targetMonth = today.format(DateTimeFormatter.ofPattern("yyyy-MM"));

        // Process new monthly payments
        processNewMonthlyPayments(today, targetMonth);

        // Process retry payments
        processRetryPayments(today, targetMonth);

        log.info("Daily payment scheduler finished.");
    }

    /**
     * Process new monthly payments for parties whose payment day is today
     * Handles edge cases for 29/30/31 day months
     *
     * @param today       Current date
     * @param targetMonth Target month in yyyy-MM format
     */
    private void processNewMonthlyPayments(LocalDate today, String targetMonth) {
        int currentDay = today.getDayOfMonth();
        int lastDayOfMonth = today.lengthOfMonth();

        log.info("Processing new monthly payments for day {} (last day: {})", currentDay, lastDayOfMonth);

        List<Party> parties = partyDao.findPartiesByPaymentDay(currentDay, lastDayOfMonth);
        log.info("Found {} parties for payment on day {}", parties.size(), currentDay);

        for (Party party : parties) {
            try {
                processPartyPayments(party, targetMonth);
            } catch (Exception e) {
                log.error("Failed to process payments for partyId: {}", party.getPartyId(), e);
                // Continue with next party (isolation)
            }
        }
    }

    /**
     * Process payments for all active members in a party
     * Includes leader and regular members
     *
     * @param party       Party to process
     * @param targetMonth Target month in yyyy-MM format
     */
    private void processPartyPayments(Party party, String targetMonth) {
        // 방장 제외 활성 멤버 조회 (방장은 월 구독료 결제하지 않음)
        List<PartyMember> members = partyMemberDao.findActiveMembersExcludingLeader(party.getPartyId());

        log.info("Processing {} active members for partyId: {}", members.size(), party.getPartyId());

        for (PartyMember member : members) {
            try {
                paymentService.processMonthlyPayment(
                        party.getPartyId(),
                        member.getPartyMemberId(),
                        member.getUserId(),
                        party.getMonthlyFee(),
                        targetMonth);
            } catch (Exception e) {
                log.error("Failed to process payment for partyMemberId: {}", member.getPartyMemberId(), e);
                // Continue with next member
            }
        }
    }

    /**
     * Process retry payments scheduled for today
     * Handles payments that failed previously and are due for retry
     *
     * @param today       Current date
     * @param targetMonth Target month in yyyy-MM format
     */
    private void processRetryPayments(LocalDate today, String targetMonth) {
        List<PaymentRetryHistory> retries = retryService.findPendingRetries(today);
        log.info("Found {} payments pending retry", retries.size());

        for (PaymentRetryHistory retry : retries) {
            try {
                retryService.retryPayment(retry, targetMonth);
            } catch (Exception e) {
                log.error("Failed to retry paymentId: {}", retry.getPaymentId(), e);
                // Continue with next retry
            }
        }
    }
}
