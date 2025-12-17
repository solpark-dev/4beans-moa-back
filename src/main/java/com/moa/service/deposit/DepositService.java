package com.moa.service.deposit;

import java.util.List;

import com.moa.domain.Deposit;
import com.moa.domain.Party;
import com.moa.dto.deposit.response.DepositResponse;
import com.moa.dto.payment.request.PaymentRequest;

public interface DepositService {

	Deposit createDeposit(Integer partyId, Integer partyMemberId, String userId, Integer amount,
			PaymentRequest request);

	Deposit createDepositWithoutConfirm(Integer partyId, Integer partyMemberId, String userId, Integer amount,
			PaymentRequest request);

	DepositResponse getDepositDetail(Integer depositId);

	List<DepositResponse> getMyDeposits(String userId);

	List<DepositResponse> getPartyDeposits(Integer partyId);

	Deposit findByPartyIdAndUserId(Integer partyId, String userId);

	void refundDeposit(Integer depositId, String reason);

	void processWithdrawalRefund(Integer depositId, Party party);

	void forfeitDeposit(Integer depositId, String reason);
}
