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
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void retryRefund(RefundRetryHistory retry) {
        log.info("Retrying refund: depositId={}, retryId={}, attemptNumber={}, retryType={}",
                retry.getDepositId(), retry.getRetryId(), retry.getAttemptNumber(), retry.getRetryType());

        // retryType에 따라 분기 처리
        if ("COMPENSATION".equals(retry.getRetryType())) {
            retryCompensation(retry);
            return;
        }

        // 기존 REFUND 로직
        retryRefundLogic(retry);
    }

    /**
     * COMPENSATION 타입 재시도 처리
     * Toss 취소만 수행하고 PENDING 상태 Deposit 삭제
     */
    private void retryCompensation(RefundRetryHistory retry) {
        log.info("Processing compensation: depositId={}, retryId={}",
                retry.getDepositId(), retry.getRetryId());

        Deposit deposit = depositDao.findById(retry.getDepositId()).orElse(null);

        // Deposit이 이미 삭제되었거나 PENDING이 아니면 성공으로 처리
        if (deposit == null || deposit.getDepositStatus() != DepositStatus.PENDING) {
            log.info("Deposit already processed or deleted: depositId={}", retry.getDepositId());
            retry.setRetryStatus("SUCCESS");
            retry.setUpdatedAt(LocalDateTime.now());
            retryDao.updateRetryStatus(retry);
            return;
        }

        int nextAttempt = retry.getAttemptNumber() + 1;

        try {
            // Toss 결제 취소
            tossPaymentService.cancelPayment(
                    deposit.getTossPaymentKey(),
                    retry.getRefundReason() != null ? retry.getRefundReason() : "보상 트랜잭션 - 자동 취소",
                    retry.getRefundAmount());

            log.info("Toss cancellation successful: depositId={}", deposit.getDepositId());

            // PENDING 상태 Deposit 삭제
            depositDao.deleteById(deposit.getDepositId());

            // 성공으로 마킹
            retry.setRetryStatus("SUCCESS");
            retry.setAttemptNumber(nextAttempt);
            retry.setAttemptDate(LocalDateTime.now());
            retry.setUpdatedAt(LocalDateTime.now());
            retryDao.updateRetryStatus(retry);

            log.info("Compensation completed successfully: depositId={}", deposit.getDepositId());

        } catch (Exception e) {
            log.error("Compensation failed: depositId={}, error={}", deposit.getDepositId(), e.getMessage());
            handleRetryFailure(retry, nextAttempt, e);
        }
    }

    /**
     * 기존 REFUND 타입 재시도 로직
     */
    private void retryRefundLogic(RefundRetryHistory retry) {
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
            handleRetryFailure(retry, nextAttempt, e);
        }
    }

    /**
     * 재시도 실패 처리 공통 로직
     */
    private void handleRetryFailure(RefundRetryHistory retry, int nextAttempt, Exception e) {
        if (nextAttempt >= MAX_RETRY_ATTEMPTS) {
            // Max attempts reached - permanent failure
            log.error("Max retry attempts reached for depositId={}", retry.getDepositId());

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

            // 4회 실패 시 관리자 알림 발송
            sendAdminNotification(retry, e);
        } else {
            // Schedule next retry
            LocalDateTime nextRetryDate = calculateNextRetryDate(nextAttempt);
            log.info("Scheduling next retry: depositId={}, nextAttempt={}, nextRetryDate={}",
                    retry.getDepositId(), nextAttempt + 1, nextRetryDate);

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

    @Override
    public LocalDateTime calculateNextRetryDate(int attemptNumber) {
        LocalDateTime now = LocalDateTime.now();

        return switch (attemptNumber) {
            case 1 -> now.plusHours(1); // After 1st attempt: +1 hour
            case 2 -> now.plusHours(4); // After 2nd attempt: +4 hours
            case 3 -> now.plusHours(24); // After 3rd attempt: +24 hours
            default -> now.plusHours(1); // Default: +1 hour
        };
    }

    /**
     * Record refund failure with REQUIRES_NEW transaction
     * Ensures failure history is preserved even if parent transaction rolls back
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(Deposit deposit, Exception e, String reason) {
        log.info("Recording refund failure: depositId={}, reason={}", deposit.getDepositId(), reason);

        String errorCode = e.getClass().getSimpleName();
        String errorMessage = e.getMessage();

        if (e instanceof com.moa.common.exception.TossPaymentException tpe) {
            errorCode = tpe.getTossErrorCode();
            errorMessage = tpe.getMessage();
        }

        RefundRetryHistory history = RefundRetryHistory.builder()
                .depositId(deposit.getDepositId())
                .attemptNumber(1)
                .attemptDate(LocalDateTime.now())
                .retryStatus("FAILED")
                .nextRetryDate(calculateNextRetryDate(1))
                .errorCode(errorCode)
                .errorMessage(errorMessage != null
                        ? errorMessage.substring(0, Math.min(errorMessage.length(), 500))
                        : "Unknown error")
                .refundAmount(deposit.getDepositAmount())
                .refundReason(reason)
                .retryType("REFUND")
                .build();

        retryDao.insertRefundRetry(history);
        log.info("Refund failure recorded: depositId={}, retryId={}", deposit.getDepositId(), history.getRetryId());
    }

    /**
     * Register compensation transaction for Toss payment cancellation
     * Used when Toss payment succeeded but DB save failed
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registerCompensation(Integer depositId, String tossPaymentKey, Integer amount, String reason) {
        log.info("Registering compensation transaction: depositId={}, amount={}, reason={}",
                depositId, amount, reason);

        RefundRetryHistory history = RefundRetryHistory.builder()
                .depositId(depositId)
                .attemptNumber(1)
                .attemptDate(LocalDateTime.now())
                .retryStatus("PENDING")
                .nextRetryDate(calculateNextRetryDate(1))
                .refundAmount(amount)
                .refundReason(reason)
                .retryType("COMPENSATION")
                .build();

        retryDao.insertRefundRetry(history);
        log.info("Compensation transaction registered: depositId={}, retryId={}", depositId, history.getRetryId());
    }

    /**
     * 보상 트랜잭션 필요 이력 기록 (REQUIRES_NEW 트랜잭션)
     * Toss 성공 후 DB 업데이트 실패 시 호출
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordCompensation(Deposit deposit, String reason) {
        log.info("Recording compensation: depositId={}, reason={}", deposit.getDepositId(), reason);

        RefundRetryHistory history = RefundRetryHistory.builder()
                .depositId(deposit.getDepositId())
                .attemptNumber(1)
                .attemptDate(LocalDateTime.now())
                .retryStatus("PENDING")
                .nextRetryDate(LocalDateTime.now().plusMinutes(5)) // 5분 후 보상 처리
                .refundAmount(deposit.getDepositAmount())
                .refundReason(reason)
                .retryType("COMPENSATION")
                .errorCode("COMPENSATION_REQUIRED")
                .errorMessage("Toss 성공 후 DB 업데이트 실패로 인한 보상 트랜잭션 필요")
                .build();

        retryDao.insertRefundRetry(history);
        log.info("Compensation recorded: depositId={}, retryId={}", deposit.getDepositId(), history.getRetryId());
    }

    /**
     * 4회 재시도 실패 시 관리자 알림 발송
     * 
     * **Feature: transaction-compensation, Task 11.1**
     * **Validates: Requirements 8.1, 8.2**
     */
    private void sendAdminNotification(RefundRetryHistory retry, Exception e) {
        log.error("=== 관리자 알림: 보상 트랜잭션 최종 실패 ===");
        log.error("depositId: {}", retry.getDepositId());
        log.error("retryId: {}", retry.getRetryId());
        log.error("retryType: {}", retry.getRetryType());
        log.error("금액: {}원", retry.getRefundAmount());
        log.error("실패 사유: {}", retry.getRefundReason());
        log.error("에러 코드: {}", e.getClass().getSimpleName());
        log.error("에러 메시지: {}", e.getMessage());
        log.error("재시도 횟수: {}", retry.getAttemptNumber());
        log.error("=== 수동 처리가 필요합니다 ===");

        // TODO: 실제 알림 발송 (이메일, Slack, SMS 등)
        // 현재는 로그로 대체
    }
}
