package com.moa.service.refund.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.moa.common.exception.BusinessException;
import com.moa.common.exception.ErrorCode;
import com.moa.dao.deposit.DepositDao;
import com.moa.dao.refund.RefundRetryHistoryDao;
import com.moa.domain.Deposit;
import com.moa.domain.RefundRetryHistory;
import com.moa.domain.enums.DepositStatus;
import com.moa.service.refund.RefundRetryService;
import com.moa.service.payment.TossPaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Refund Retry Service Implementation
 * Handles automatic retry logic for failed deposit refunds
 *
 * @author MOA Team
 * @since 2025-12-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundRetryServiceImpl implements RefundRetryService {

    private static final int MAX_RETRY_ATTEMPTS = 4;

    private final RefundRetryHistoryDao retryDao;
    private final DepositDao depositDao;
    private final TossPaymentService tossPaymentService;

    @Override
    @Transactional(readOnly = true)
    public List<RefundRetryHistory> findPendingRetries() {
        log.info("Finding pending refund retries");

        List<RefundRetryHistory> retries = retryDao.findPendingRetries();
        log.info("Found {} pending refund retries", retries.size());

        return retries;
    }

    @Override
    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            isolation = Isolation.READ_COMMITTED,
            rollbackFor = Exception.class)
    public void retryRefund(RefundRetryHistory retry) {
        log.info("Retrying refund: depositId={}, retryId={}, attemptNumber={}",
                retry.getDepositId(), retry.getRetryId(), retry.getAttemptNumber());

        // 1. Load deposit
        Deposit deposit = depositDao.findById(retry.getDepositId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DEPOSIT_NOT_FOUND));

        // 2. Check if already refunded (defensive check)
        if (deposit.getDepositStatus() == DepositStatus.REFUNDED) {
            log.warn("Deposit already refunded, marking retry as success: depositId={}",
                    deposit.getDepositId());

            retry.setRetryStatus("SUCCESS");
            retry.setUpdatedAt(LocalDateTime.now());
            retryDao.updateRetryStatus(retry);
            return;
        }

        // 3. Check if deposit can be refunded
        if (deposit.getDepositStatus() != DepositStatus.PAID) {
            log.error("Deposit cannot be refunded, status={}: depositId={}",
                    deposit.getDepositStatus(), deposit.getDepositId());

            retry.setRetryStatus("FAILED");
            retry.setErrorMessage("Deposit status is not PAID: " + deposit.getDepositStatus());
            retry.setUpdatedAt(LocalDateTime.now());
            retryDao.updateRetryStatus(retry);
            return;
        }

        // 4. Attempt refund
        int nextAttempt = retry.getAttemptNumber() + 1;

        try {
            log.info("Attempting Toss refund: depositId={}, attempt={}, paymentKey={}",
                    deposit.getDepositId(), nextAttempt, deposit.getTossPaymentKey());

            // Call Toss Payments API to cancel
            tossPaymentService.cancelPayment(
                    deposit.getTossPaymentKey(),
                    retry.getRefundReason() != null ? retry.getRefundReason() : "보증금 환불 재시도",
                    retry.getRefundAmount());

            log.info("Toss refund successful: depositId={}, attempt={}",
                    deposit.getDepositId(), nextAttempt);

            // Update deposit status
            deposit.setDepositStatus(DepositStatus.REFUNDED);
            deposit.setRefundDate(LocalDateTime.now());
            deposit.setRefundAmount(retry.getRefundAmount());
            depositDao.updateDeposit(deposit);

            // Mark retry as successful
            retry.setRetryStatus("SUCCESS");
            retry.setAttemptNumber(nextAttempt);
            retry.setAttemptDate(LocalDateTime.now());
            retry.setUpdatedAt(LocalDateTime.now());
            retryDao.updateRetryStatus(retry);

            log.info("Refund retry completed successfully: depositId={}, attempt={}",
                    deposit.getDepositId(), nextAttempt);

        } catch (Exception e) {
            log.error("Refund retry failed: depositId={}, attempt={}, error={}",
                    deposit.getDepositId(), nextAttempt, e.getMessage(), e);

            if (nextAttempt >= MAX_RETRY_ATTEMPTS) {
                // Max attempts reached - permanent failure
                log.error("Max retry attempts reached for deposit: depositId={}", deposit.getDepositId());

                retry.setRetryStatus("FAILED");
                retry.setAttemptNumber(nextAttempt);
                retry.setAttemptDate(LocalDateTime.now());
                retry.setNextRetryDate(null); // No more retries
                retry.setErrorCode(e.getClass().getSimpleName());
                retry.setErrorMessage(
                        e.getMessage() != null ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 500))
                                : "Unknown error");
                retry.setUpdatedAt(LocalDateTime.now());
                retryDao.updateRetryStatus(retry);
            } else {
                // Schedule next retry
                LocalDateTime nextRetryDate = calculateNextRetryDate(nextAttempt);
                log.info("Scheduling next retry: depositId={}, nextAttempt={}, nextRetryDate={}",
                        deposit.getDepositId(), nextAttempt + 1, nextRetryDate);

                retry.setRetryStatus("FAILED");
                retry.setAttemptNumber(nextAttempt);
                retry.setAttemptDate(LocalDateTime.now());
                retry.setNextRetryDate(nextRetryDate);
                retry.setErrorCode(e.getClass().getSimpleName());
                retry.setErrorMessage(
                        e.getMessage() != null ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 500))
                                : "Unknown error");
                retry.setUpdatedAt(LocalDateTime.now());
                retryDao.updateRetryStatus(retry);
            }
        }
    }

    @Override
    public LocalDateTime calculateNextRetryDate(int attemptNumber) {
        LocalDateTime now = LocalDateTime.now();

        return switch (attemptNumber) {
            case 1 -> now.plusHours(1);      // After 1st attempt: +1 hour
            case 2 -> now.plusHours(4);      // After 2nd attempt: +4 hours
            case 3 -> now.plusHours(24);     // After 3rd attempt: +24 hours
            default -> now.plusHours(1);     // Default: +1 hour
        };
    }
}
