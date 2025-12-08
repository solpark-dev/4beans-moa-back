package com.moa.service.payment.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moa.common.exception.BusinessException;
import com.moa.common.exception.ErrorCode;
import com.moa.dao.payment.PaymentDao;
import com.moa.dao.payment.PaymentRetryDao;
import com.moa.domain.Payment;
import com.moa.domain.PaymentRetryHistory;
import com.moa.domain.enums.PaymentStatus;
import com.moa.service.payment.PaymentRetryService;
import com.moa.service.payment.PaymentService;

import lombok.extern.slf4j.Slf4j;

/**
 * Payment Retry Service Implementation
 * Handles payment retry logic with exponential backoff
 *
 * @author MOA Team
 * @since 2025-12-04
 */
@Service
@Transactional
@Slf4j
public class PaymentRetryServiceImpl implements PaymentRetryService {

    private final PaymentRetryDao retryDao;
    private final PaymentDao paymentDao;
    private final PaymentService paymentService;

    /**
     * Constructor with @Lazy injection for PaymentService to break circular dependency
     * Circular dependency: PaymentService → PaymentRetryService → PaymentService
     */
    public PaymentRetryServiceImpl(
            PaymentRetryDao retryDao,
            PaymentDao paymentDao,
            @Lazy PaymentService paymentService) {
        this.retryDao = retryDao;
        this.paymentDao = paymentDao;
        this.paymentService = paymentService;
    }

    @Override
    public void recordSuccess(Payment payment, int attemptNumber) {
        log.info("Recording successful payment attempt: paymentId={}, attemptNumber={}",
                payment.getPaymentId(), attemptNumber);

        PaymentRetryHistory history = PaymentRetryHistory.builder()
                .paymentId(payment.getPaymentId())
                .partyId(payment.getPartyId())
                .partyMemberId(payment.getPartyMemberId())
                .attemptNumber(attemptNumber)
                .attemptDate(LocalDateTime.now())
                .retryStatus("SUCCESS")
                .nextRetryDate(null) // No retry needed
                .build();

        retryDao.insert(history);
        log.debug("Retry history recorded: retryId={}", history.getRetryId());
    }

    @Override
    public void recordFailureWithRetry(
            Payment payment,
            int attemptNumber,
            String errorCode,
            String errorMessage,
            LocalDateTime nextRetryDate) {

        log.warn("Recording failed payment attempt with retry: paymentId={}, attemptNumber={}, nextRetry={}",
                payment.getPaymentId(), attemptNumber, nextRetryDate);

        PaymentRetryHistory history = PaymentRetryHistory.builder()
                .paymentId(payment.getPaymentId())
                .partyId(payment.getPartyId())
                .partyMemberId(payment.getPartyMemberId())
                .attemptNumber(attemptNumber)
                .attemptDate(LocalDateTime.now())
                .retryStatus("FAILED")
                .retryReason(errorMessage)
                .nextRetryDate(nextRetryDate)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();

        retryDao.insert(history);
        log.info("Payment retry scheduled: retryId={}, nextRetry={}",
                history.getRetryId(), nextRetryDate);
    }

    @Override
    public void recordPermanentFailure(
            Payment payment,
            int attemptNumber,
            BusinessException exception) {

        log.error("Recording permanent payment failure: paymentId={}, attemptNumber={}",
                payment.getPaymentId(), attemptNumber);

        PaymentRetryHistory history = PaymentRetryHistory.builder()
                .paymentId(payment.getPaymentId())
                .partyId(payment.getPartyId())
                .partyMemberId(payment.getPartyMemberId())
                .attemptNumber(attemptNumber)
                .attemptDate(LocalDateTime.now())
                .retryStatus("FAILED")
                .retryReason("Max retry attempts exceeded")
                .nextRetryDate(null) // No more retries
                .errorCode(exception.getErrorCode().getCode())
                .errorMessage(exception.getMessage())
                .build();

        retryDao.insert(history);
        log.info("Permanent failure recorded: retryId={}", history.getRetryId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentRetryHistory> findPendingRetries(LocalDate today) {
        log.info("Finding pending retries for date: {}", today);

        List<PaymentRetryHistory> retries = retryDao.findByNextRetryDate(today);
        log.info("Found {} pending retries", retries.size());

        return retries;
    }

    @Override
    public void retryPayment(PaymentRetryHistory retry, String targetMonth) {
        log.info("Retrying payment: paymentId={}, attemptNumber={}",
                retry.getPaymentId(), retry.getAttemptNumber());

        // 1. Load payment
        Payment payment = paymentDao.findById(retry.getPaymentId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        // 2. Check if already completed (defensive check)
        if (payment.getPaymentStatus() == PaymentStatus.COMPLETED) {
            log.warn("Payment already completed, skipping retry: paymentId={}",
                    payment.getPaymentId());
            return;
        }

        // 3. Attempt payment again with incremented attempt number
        int nextAttempt = retry.getAttemptNumber() + 1;
        log.info("Attempting payment execution: paymentId={}, attempt={}",
                payment.getPaymentId(), nextAttempt);

        paymentService.attemptPaymentExecution(payment, nextAttempt);
    }
}
