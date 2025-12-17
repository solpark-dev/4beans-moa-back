package com.moa.service.settlement.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moa.common.exception.BusinessException;
import com.moa.common.exception.ErrorCode;
import com.moa.dao.account.AccountDao;
import com.moa.dao.deposit.DepositDao;
import com.moa.dao.party.PartyDao;
import com.moa.dao.payment.PaymentDao;
import com.moa.dao.settlement.SettlementDao;
import com.moa.domain.Account;
import com.moa.domain.Deposit;
import com.moa.domain.Party;
import com.moa.domain.Settlement;
import com.moa.domain.enums.SettlementStatus;
import com.moa.dto.payment.response.PaymentResponse;
import com.moa.dto.settlement.response.SettlementDetailResponse;
import com.moa.dto.settlement.response.SettlementResponse;
import com.moa.service.openbanking.OpenBankingService;
import com.moa.service.settlement.SettlementService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SettlementServiceImpl implements SettlementService {

	private final SettlementDao settlementDao;
	private final PaymentDao paymentDao;
	private final PartyDao partyDao;
	private final AccountDao accountDao;
	private final DepositDao depositDao;
	private final OpenBankingService openBankingService;

	private static final double COMMISSION_RATE = 0.15;

	@Override
	public Settlement createMonthlySettlement(Integer partyId, String targetMonth) {
		if (settlementDao.findByPartyIdAndMonth(partyId, targetMonth).isPresent()) {
			throw new BusinessException(ErrorCode.DUPLICATE_SETTLEMENT);
		}
		Party party = partyDao.findById(partyId).orElseThrow(() -> new BusinessException(ErrorCode.PARTY_NOT_FOUND));

		Account account = accountDao.findByUserId(party.getPartyLeaderId()).orElse(null);

		if (account == null) {
			log.warn("정산 계좌 미등록: partyId={}, leaderId={}", partyId, party.getPartyLeaderId());
			return createPendingAccountSettlement(partyId, party, targetMonth);
		}

		LocalDateTime partyStartDate = party.getStartDate();
		if (partyStartDate == null) {
			throw new BusinessException(ErrorCode.START_DATE_REQUIRED);
		}

		int billingDay = partyStartDate.getDayOfMonth();
		LocalDateTime tempStartDate;
		LocalDateTime tempEndDate;

		String[] monthParts = targetMonth.split("-");
		int targetYear = Integer.parseInt(monthParts[0]);
		int targetMonthNum = Integer.parseInt(monthParts[1]);

		if (billingDay == 1) {
			tempStartDate = LocalDateTime.of(targetYear, targetMonthNum, 1, 0, 0, 0);
			tempEndDate = tempStartDate.plusMonths(1).minusDays(1).withHour(23).withMinute(59).withSecond(59);
		} else {
			tempStartDate = LocalDateTime.of(targetYear, targetMonthNum, billingDay, 0, 0, 0);
			tempEndDate = tempStartDate.plusMonths(1).minusDays(1).withHour(23).withMinute(59).withSecond(59);
		}

		final LocalDateTime settlementStartDate = tempStartDate.isBefore(partyStartDate) ? partyStartDate
				: tempStartDate;
		final LocalDateTime settlementEndDate = tempEndDate;
		LocalDateTime now = LocalDateTime.now();
		if (settlementEndDate.isAfter(now)) {
			log.warn("정산 기간이 아직 완료되지 않음: partyId={}, targetMonth={}, endDate={}", partyId, targetMonth,
					settlementEndDate);
			throw new BusinessException(ErrorCode.SETTLEMENT_PERIOD_NOT_COMPLETED);
		}

		List<PaymentResponse> payments = paymentDao.findByPartyId(partyId);

		List<PaymentResponse> targetPayments = payments.stream().filter(p -> "COMPLETED".equals(p.getPaymentStatus()))
				.filter(p -> p.getPaymentDate() != null).filter(p -> !p.getPaymentDate().isBefore(settlementStartDate)
						&& !p.getPaymentDate().isAfter(settlementEndDate))
				.collect(Collectors.toList());

		List<Deposit> forfeitedDeposits = depositDao.findForfeitedByPartyIdAndPeriod(partyId, settlementStartDate,
				settlementEndDate);

		int forfeitedAmount = forfeitedDeposits.stream().mapToInt(Deposit::getDepositAmount).sum();

		if (targetPayments.isEmpty() && forfeitedDeposits.isEmpty()) {
			return null;
		}

		int paymentTotal = targetPayments.stream().mapToInt(PaymentResponse::getPaymentAmount).sum();
		int totalAmount = paymentTotal + forfeitedAmount;
		int commissionAmount = (int) (paymentTotal * COMMISSION_RATE);
		int netAmount = totalAmount - commissionAmount;

		Settlement settlement = Settlement.builder().partyId(partyId).partyLeaderId(party.getPartyLeaderId())
				.accountId(account.getAccountId()).settlementMonth(targetMonth).settlementType("MONTHLY")
				.totalAmount(totalAmount).commissionRate(COMMISSION_RATE).commissionAmount(commissionAmount)
				.netAmount(netAmount).settlementStatus(SettlementStatus.PENDING).regDate(LocalDateTime.now()).build();

		settlementDao.insertSettlement(settlement);
		for (PaymentResponse p : targetPayments) {
			paymentDao.updateSettlementId(p.getPaymentId(), settlement.getSettlementId());
		}

		return settlement;
	}

	@Override
	public void completeSettlement(Integer settlementId) {
		Settlement settlement = settlementDao.findById(settlementId)
				.orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));
		if (settlement.getSettlementStatus() == SettlementStatus.COMPLETED) {
			throw new BusinessException(ErrorCode.SETTLEMENT_ALREADY_COMPLETED);
		}
		if (settlement.getSettlementStatus() == SettlementStatus.IN_PROGRESS) {
			throw new BusinessException(ErrorCode.SETTLEMENT_FAILED);
		}
		Account account = accountDao.findById(settlement.getAccountId())
				.orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
		if (!"Y".equals(account.getIsVerified())) {
			throw new BusinessException(ErrorCode.ACCOUNT_NOT_VERIFIED);
		}

		String bankTranId = null;
		try {
			settlementDao.updateSettlementStatus(settlementId, SettlementStatus.IN_PROGRESS.name(), null);
			settlement.setSettlementStatus(SettlementStatus.IN_PROGRESS);
			bankTranId = openBankingService.depositToUser(account.getBankCode(), account.getAccountNumber(),
					account.getAccountHolder(), settlement.getNetAmount());

			settlementDao.updateSettlementStatus(settlementId, SettlementStatus.COMPLETED.name(), bankTranId);
			settlement.setSettlementStatus(SettlementStatus.COMPLETED);
			settlement.setBankTranId(bankTranId);

		} catch (Exception e) {
			String finalBankTranId = bankTranId != null ? bankTranId : null;
			settlementDao.updateSettlementStatus(settlementId, SettlementStatus.FAILED.name(), finalBankTranId);
			settlement.setSettlementStatus(SettlementStatus.FAILED);

			throw new BusinessException(ErrorCode.SETTLEMENT_FAILED);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public List<SettlementResponse> getSettlementsByLeaderId(String leaderId) {
		return settlementDao.findByLeaderId(leaderId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<SettlementDetailResponse> getSettlementDetails(Integer settlementId) {
		List<PaymentResponse> payments = paymentDao.findBySettlementId(settlementId);

		return payments.stream()
				.map(p -> SettlementDetailResponse.builder().detailId(null).settlementId(settlementId)
						.paymentId(p.getPaymentId()).userId(p.getUserId()).userNickname(p.getUserNickname())

						.paymentAmount(p.getPaymentAmount()).regDate(p.getPaymentDate()).build())
				.collect(Collectors.toList());
	}

	private Settlement createPendingAccountSettlement(Integer partyId, Party party, String targetMonth) {
		log.info("계좌 미등록으로 PENDING_ACCOUNT 정산 생성: partyId={}, targetMonth={}", partyId, targetMonth);

		List<PaymentResponse> payments = paymentDao.findByPartyId(partyId);
		List<PaymentResponse> targetPayments = payments.stream().filter(p -> "COMPLETED".equals(p.getPaymentStatus()))
				.filter(p -> targetMonth.equals(p.getTargetMonth())).collect(Collectors.toList());

		LocalDateTime partyStartDate = party.getStartDate();
		LocalDateTime settlementStartDate = partyStartDate != null ? partyStartDate
				: LocalDateTime.now().minusMonths(1);
		LocalDateTime settlementEndDate = LocalDateTime.now();

		List<Deposit> forfeitedDeposits = depositDao.findForfeitedByPartyIdAndPeriod(partyId, settlementStartDate,
				settlementEndDate);

		int forfeitedAmount = forfeitedDeposits.stream().mapToInt(Deposit::getDepositAmount).sum();

		if (targetPayments.isEmpty() && forfeitedDeposits.isEmpty()) {
			log.info("정산할 내역 없음: partyId={}", partyId);
			return null;
		}

		int paymentTotal = targetPayments.stream().mapToInt(PaymentResponse::getPaymentAmount).sum();
		int totalAmount = paymentTotal + forfeitedAmount;
		int commissionAmount = (int) (paymentTotal * COMMISSION_RATE);
		int netAmount = totalAmount - commissionAmount;

		Settlement settlement = Settlement.builder().partyId(partyId).partyLeaderId(party.getPartyLeaderId())
				.accountId(null) // 계좌 미등록
				.settlementMonth(targetMonth).settlementType("MONTHLY").totalAmount(totalAmount)
				.commissionRate(COMMISSION_RATE).commissionAmount(commissionAmount).netAmount(netAmount)
				.settlementStatus(SettlementStatus.PENDING_ACCOUNT).regDate(LocalDateTime.now()).build();

		settlementDao.insertSettlement(settlement);

		for (PaymentResponse p : targetPayments) {
			paymentDao.updateSettlementId(p.getPaymentId(), settlement.getSettlementId());
		}
		sendAccountRequiredPush(party.getPartyLeaderId(), partyId, netAmount);

		log.info("PENDING_ACCOUNT 정산 생성 완료: settlementId={}, netAmount={}", settlement.getSettlementId(), netAmount);

		return settlement;
	}

	private void sendAccountRequiredPush(String leaderId, Integer partyId, int netAmount) {
		try {
			log.info("계좌 등록 요청 알림: leaderId={}, partyId={}, 정산금액={}원", leaderId, partyId, netAmount);
		} catch (Exception e) {
			log.error("푸시 발송 실패: {}", e.getMessage());
		}
	}
}
