package com.moa.service.refund;

import java.time.LocalDateTime;
import java.util.List;

import com.moa.domain.RefundRetryHistory;

public interface RefundRetryService {

	List<RefundRetryHistory> findPendingRetries();

	void retryRefund(RefundRetryHistory retry);

	LocalDateTime calculateNextRetryDate(int attemptNumber);

	void recordFailure(com.moa.domain.Deposit deposit, Exception e, String reason);

	void registerCompensation(Integer depositId, String tossPaymentKey, Integer amount, String reason);

	void recordCompensation(com.moa.domain.Deposit deposit, String reason);
}
