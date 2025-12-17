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
import com.moa.service.payment.TossPaymentService;
import com.moa.service.refund.RefundRetryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
		log.info("Retrying refund: depositId={}, retryId={}, attemptNumber={}, retryType={}", retry.getDepositId(),
				retry.getRetryId(), retry.getAttemptNumber(), retry.getRetryType());

		if ("COMPENSATION".equals(retry.getRetryType())) {
			retryCompensation(retry);
			return;
		}

		retryRefundLogic(retry);
	}

	private void retryCompensation(RefundRetryHistory retry) {
		log.info("Processing compensation: depositId={}, retryId={}", retry.getDepositId(), retry.getRetryId());

		Deposit deposit = depositDao.findById(retry.getDepositId()).orElse(null);

		if (deposit == null || deposit.getDepositStatus() != DepositStatus.PENDING) {
			log.info("Deposit already processed or deleted: depositId={}", retry.getDepositId());
			retry.setRetryStatus("SUCCESS");
			retry.setUpdatedAt(LocalDateTime.now());
			retryDao.updateRetryStatus(retry);
			return;
		}

		int nextAttempt = retry.getAttemptNumber() + 1;

		try {
			tossPaymentService.cancelPayment(deposit.getTossPaymentKey(),
					retry.getRefundReason() != null ? retry.getRefundReason() : "보상 트랜잭션 - 자동 취소",
					retry.getRefundAmount());

			log.info("Toss cancellation successful: depositId={}", deposit.getDepositId());

			depositDao.deleteById(deposit.getDepositId());

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

	private void retryRefundLogic(RefundRetryHistory retry) {
		Deposit deposit = depositDao.findById(retry.getDepositId())
				.orElseThrow(() -> new BusinessException(ErrorCode.DEPOSIT_NOT_FOUND));

		if (deposit.getDepositStatus() == DepositStatus.REFUNDED) {
			log.warn("Deposit already refunded, marking retry as success: depositId={}", deposit.getDepositId());

			retry.setRetryStatus("SUCCESS");
			retry.setUpdatedAt(LocalDateTime.now());
			retryDao.updateRetryStatus(retry);
			return;
		}

		if (deposit.getDepositStatus() != DepositStatus.PAID) {
			log.error("Deposit cannot be refunded, status={}: depositId={}", deposit.getDepositStatus(),
					deposit.getDepositId());

			retry.setRetryStatus("FAILED");
			retry.setErrorMessage("Deposit status is not PAID: " + deposit.getDepositStatus());
			retry.setUpdatedAt(LocalDateTime.now());
			retryDao.updateRetryStatus(retry);
			return;
		}

		int nextAttempt = retry.getAttemptNumber() + 1;

		try {
			log.info("Attempting Toss refund: depositId={}, attempt={}, paymentKey={}", deposit.getDepositId(),
					nextAttempt, deposit.getTossPaymentKey());

			tossPaymentService.cancelPayment(deposit.getTossPaymentKey(),
					retry.getRefundReason() != null ? retry.getRefundReason() : "보증금 환불 재시도", retry.getRefundAmount());

			log.info("Toss refund successful: depositId={}, attempt={}", deposit.getDepositId(), nextAttempt);

			deposit.setDepositStatus(DepositStatus.REFUNDED);
			deposit.setRefundDate(LocalDateTime.now());
			deposit.setRefundAmount(retry.getRefundAmount());
			depositDao.updateDeposit(deposit);

			retry.setRetryStatus("SUCCESS");
			retry.setAttemptNumber(nextAttempt);
			retry.setAttemptDate(LocalDateTime.now());
			retry.setUpdatedAt(LocalDateTime.now());
			retryDao.updateRetryStatus(retry);

			log.info("Refund retry completed successfully: depositId={}, attempt={}", deposit.getDepositId(),
					nextAttempt);

		} catch (Exception e) {
			log.error("Refund retry failed: depositId={}, attempt={}, error={}", deposit.getDepositId(), nextAttempt,
					e.getMessage(), e);
			handleRetryFailure(retry, nextAttempt, e);
		}
	}

	private void handleRetryFailure(RefundRetryHistory retry, int nextAttempt, Exception e) {
		String errorCode = classifyError(e);
		boolean isRetryable = isRetryableError(errorCode);

		if (!isRetryable) {
			log.error("재시도 불가능한 에러 발생, 즉시 최종 실패 처리: depositId={}, errorCode={}", retry.getDepositId(), errorCode);
			handlePermanentFailure(retry, nextAttempt, e, errorCode);
			return;
		}
		if (nextAttempt >= MAX_RETRY_ATTEMPTS) {
			log.error("Max retry attempts reached for depositId={}", retry.getDepositId());
			handlePermanentFailure(retry, nextAttempt, e, errorCode);
		} else {
			LocalDateTime nextRetryDate = calculateNextRetryDate(nextAttempt);
			log.info("Scheduling next retry: depositId={}, nextAttempt={}, nextRetryDate={}", retry.getDepositId(),
					nextAttempt + 1, nextRetryDate);

			retry.setRetryStatus("PENDING");
			retry.setAttemptNumber(nextAttempt);
			retry.setAttemptDate(LocalDateTime.now());
			retry.setNextRetryDate(nextRetryDate);
			retry.setErrorCode(errorCode);
			retry.setErrorMessage(
					e.getMessage() != null ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 500))
							: "Unknown error");
			retry.setUpdatedAt(LocalDateTime.now());
			retryDao.updateRetryStatus(retry);
		}
	}

	private void handlePermanentFailure(RefundRetryHistory retry, int attemptNumber, Exception e, String errorCode) {
		retry.setRetryStatus("FAILED");
		retry.setAttemptNumber(attemptNumber);
		retry.setAttemptDate(LocalDateTime.now());
		retry.setNextRetryDate(null); // No more retries
		retry.setErrorCode(errorCode);
		retry.setErrorMessage(
				e.getMessage() != null ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 500))
						: "Unknown error");
		retry.setUpdatedAt(LocalDateTime.now());
		retryDao.updateRetryStatus(retry);

		sendAdminNotification(retry, e);
	}

	private String classifyError(Exception e) {
		if (e instanceof com.moa.common.exception.TossPaymentException tpe) {
			return tpe.getTossErrorCode();
		}

		return e.getClass().getSimpleName();
	}

	private boolean isRetryableError(String errorCode) {
		if (errorCode == null) {
			return true;
		}

		return switch (errorCode) {
		case "ALREADY_CANCELED", "ALREADY_REFUNDED", "INVALID_CANCEL_AMOUNT", "NOT_CANCELABLE_PAYMENT",
				"EXCEED_CANCEL_AMOUNT", "INVALID_PAYMENT_KEY", "NOT_FOUND_PAYMENT", "FORBIDDEN_REQUEST",
				"INVALID_REQUEST", "UNAUTHORIZED_KEY", "CANCEL_PERIOD_EXPIRED" ->
			false;
		default -> true;
		};
	}

	@Override
	public LocalDateTime calculateNextRetryDate(int attemptNumber) {
		LocalDateTime now = LocalDateTime.now();

		return switch (attemptNumber) {
		case 1 -> now.plusHours(1);
		case 2 -> now.plusHours(4);
		case 3 -> now.plusHours(24);
		default -> now.plusHours(1);
		};
	}

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

		RefundRetryHistory history = RefundRetryHistory.builder().depositId(deposit.getDepositId()).attemptNumber(1)
				.attemptDate(LocalDateTime.now()).retryStatus("FAILED").nextRetryDate(calculateNextRetryDate(1))
				.errorCode(errorCode)
				.errorMessage(errorMessage != null ? errorMessage.substring(0, Math.min(errorMessage.length(), 500))
						: "Unknown error")
				.refundAmount(deposit.getDepositAmount()).refundReason(reason).retryType("REFUND").build();

		retryDao.insertRefundRetry(history);
		log.info("Refund failure recorded: depositId={}, retryId={}", deposit.getDepositId(), history.getRetryId());
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void registerCompensation(Integer depositId, String tossPaymentKey, Integer amount, String reason) {
		log.info("Registering compensation transaction: depositId={}, amount={}, reason={}", depositId, amount, reason);

		RefundRetryHistory history = RefundRetryHistory.builder().depositId(depositId).attemptNumber(1)
				.attemptDate(LocalDateTime.now()).retryStatus("PENDING").nextRetryDate(calculateNextRetryDate(1))
				.refundAmount(amount).refundReason(reason).retryType("COMPENSATION").build();

		retryDao.insertRefundRetry(history);
		log.info("Compensation transaction registered: depositId={}, retryId={}", depositId, history.getRetryId());
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void recordCompensation(Deposit deposit, String reason) {
		log.info("Recording compensation: depositId={}, reason={}", deposit.getDepositId(), reason);

		RefundRetryHistory history = RefundRetryHistory.builder().depositId(deposit.getDepositId()).attemptNumber(1)
				.attemptDate(LocalDateTime.now()).retryStatus("PENDING")
				.nextRetryDate(LocalDateTime.now().plusMinutes(5)).refundAmount(deposit.getDepositAmount())
				.refundReason(reason).retryType("COMPENSATION").errorCode("COMPENSATION_REQUIRED")
				.errorMessage("Toss 성공 후 DB 업데이트 실패로 인한 보상 트랜잭션 필요").build();

		retryDao.insertRefundRetry(history);
		log.info("Compensation recorded: depositId={}, retryId={}", deposit.getDepositId(), history.getRetryId());
	}

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

	}
}
