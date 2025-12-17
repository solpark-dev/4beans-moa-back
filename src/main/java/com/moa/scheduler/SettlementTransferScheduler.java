package com.moa.scheduler;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.moa.dao.account.AccountDao;
import com.moa.dao.openbanking.TransferTransactionMapper;
import com.moa.dao.party.PartyDao;
import com.moa.dao.product.ProductDao;
import com.moa.dao.settlement.SettlementDao;
import com.moa.domain.Account;
import com.moa.domain.Party;
import com.moa.domain.Product;
import com.moa.domain.Settlement;
import com.moa.domain.enums.PushCodeType;
import com.moa.domain.openbanking.TransactionStatus;
import com.moa.domain.openbanking.TransferTransaction;
import com.moa.dto.openbanking.TransferDepositRequest;
import com.moa.dto.openbanking.TransferDepositResponse;
import com.moa.dto.push.request.TemplatePushRequest;
import com.moa.service.openbanking.OpenBankingClient;
import com.moa.service.push.PushService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementTransferScheduler {

	private final SettlementDao settlementDao;
	private final AccountDao accountDao;
	private final TransferTransactionMapper transactionMapper;
	private final OpenBankingClient openBankingClient;
	private final PushService pushService;
	private final PartyDao partyDao;
	private final ProductDao productDao;

	@Scheduled(cron = "0 0 10 * * *")
	public void processSettlementTransfers() {
		log.info("[정산스케줄러] 자동 이체 처리 시작");
		List<Settlement> pendingSettlements = settlementDao.findByStatus("PENDING");

		if (pendingSettlements.isEmpty()) {
			log.info("[정산스케줄러] 처리할 정산 건이 없습니다");
			return;
		}

		log.info("[정산스케줄러] 처리 대상: {}건", pendingSettlements.size());

		int successCount = 0;
		int failCount = 0;
		int skipCount = 0;

		for (Settlement settlement : pendingSettlements) {
			try {
				boolean result = processSettlement(settlement);
				if (result) {
					successCount++;
				} else {
					skipCount++;
				}
			} catch (Exception e) {
				log.error("[정산스케줄러] 정산 처리 실패 - settlementId: {}", settlement.getSettlementId(), e);
				failCount++;
			}
		}

		log.info("[정산스케줄러] 처리 완료 - 성공: {}, 실패: {}, 건너뜀: {}", successCount, failCount, skipCount);
	}

	@Transactional
	public boolean processSettlement(Settlement settlement) {
		log.info("[정산처리] 시작 - settlementId: {}, 파티장: {}, 금액: {}", settlement.getSettlementId(),
				settlement.getPartyLeaderId(), settlement.getNetAmount());

		Optional<Account> accountOpt = accountDao.findActiveByUserId(settlement.getPartyLeaderId());

		if (accountOpt.isEmpty()) {
			log.warn("[정산처리] 계좌 미등록 - 파티장: {}", settlement.getPartyLeaderId());

			sendAccountRequiredPush(settlement);

			return false;
		}

		Account account = accountOpt.get();

		if (account.getFintechUseNum() == null || account.getFintechUseNum().isBlank()) {
			log.warn("[정산처리] 핀테크번호 없음 - 파티장: {}", settlement.getPartyLeaderId());
			sendAccountRequiredPush(settlement);
			return false;
		}

		settlementDao.updateStatus(settlement.getSettlementId(), "IN_PROGRESS");

		TransferDepositRequest request = TransferDepositRequest.builder().fintechUseNum(account.getFintechUseNum())
				.tranAmt(settlement.getNetAmount()).printContent("MOA정산").reqClientName("MOA").build();

		TransferDepositResponse response = openBankingClient.transferDeposit(request);

		TransferTransaction transaction = TransferTransaction.builder().settlementId(settlement.getSettlementId())
				.bankTranId(response.getBankTranId()).fintechUseNum(account.getFintechUseNum())
				.tranAmt(settlement.getNetAmount()).printContent("MOA정산").reqClientName("MOA")
				.rspCode(response.getRspCode()).rspMessage(response.getRspMessage())
				.status("A0000".equals(response.getRspCode()) ? TransactionStatus.SUCCESS : TransactionStatus.FAILED)
				.build();

		transactionMapper.insert(transaction);
		if ("A0000".equals(response.getRspCode())) {
			settlementDao.updateStatus(settlement.getSettlementId(), "COMPLETED");
			settlementDao.updateBankTranId(settlement.getSettlementId(), response.getBankTranId());
			log.info("[정산처리] 성공 - settlementId: {}, 거래ID: {}", settlement.getSettlementId(), response.getBankTranId());

			sendSettlementCompletedPush(settlement);

			return true;
		} else {
			settlementDao.updateStatus(settlement.getSettlementId(), "FAILED");
			log.error("[정산처리] 실패 - settlementId: {}, 에러: {}", settlement.getSettlementId(), response.getRspMessage());

			sendSettlementFailedPush(settlement, response.getRspMessage());
			return false;
		}
	}

	@Transactional
	public boolean processSettlementManually(Integer settlementId) {
		Settlement settlement = settlementDao.findById(settlementId).orElse(null);
		if (settlement == null) {
			log.error("[수동정산] 정산 정보 없음 - settlementId: {}", settlementId);
			return false;
		}
		return processSettlement(settlement);
	}

	private String getProductName(Integer productId) {
		if (productId == null)
			return "OTT 서비스";

		try {
			Product product = productDao.getProduct(productId);
			return (product != null && product.getProductName() != null) ? product.getProductName() : "OTT 서비스";
		} catch (Exception e) {
			log.warn("상품 조회 실패: productId={}", productId);
			return "OTT 서비스";
		}
	}

	private void sendSettlementCompletedPush(Settlement settlement) {
		try {
			Party party = partyDao.findById(settlement.getPartyId()).orElse(null);
			if (party == null)
				return;

			String productName = getProductName(party.getProductId());

			Map<String, String> params = Map.of("productName", productName, "amount",
					String.valueOf(settlement.getNetAmount()), "targetMonth", settlement.getSettlementMonth());

			TemplatePushRequest pushRequest = TemplatePushRequest.builder().receiverId(settlement.getPartyLeaderId())
					.pushCode(PushCodeType.SETTLE_COMPLETED.getCode()).params(params)
					.moduleId(String.valueOf(settlement.getSettlementId()))
					.moduleType(PushCodeType.SETTLE_COMPLETED.getModuleType()).build();

			pushService.addTemplatePush(pushRequest);
			log.info("푸시알림 발송 완료: SETTLE_COMPLETED -> userId={}", settlement.getPartyLeaderId());

		} catch (Exception e) {
			log.error("푸시알림 발송 실패: settlementId={}, error={}", settlement.getSettlementId(), e.getMessage());
		}
	}

	private void sendSettlementFailedPush(Settlement settlement, String errorMessage) {
		try {
			Party party = partyDao.findById(settlement.getPartyId()).orElse(null);
			if (party == null)
				return;

			String productName = getProductName(party.getProductId());

			Map<String, String> params = Map.of("productName", productName, "amount",
					String.valueOf(settlement.getNetAmount()), "errorMessage",
					errorMessage != null ? errorMessage : "이체 처리 중 오류가 발생했습니다.");

			TemplatePushRequest pushRequest = TemplatePushRequest.builder().receiverId(settlement.getPartyLeaderId())
					.pushCode(PushCodeType.SETTLE_FAILED.getCode()).params(params)
					.moduleId(String.valueOf(settlement.getSettlementId()))
					.moduleType(PushCodeType.SETTLE_FAILED.getModuleType()).build();

			pushService.addTemplatePush(pushRequest);
			log.info("푸시알림 발송 완료: SETTLE_FAILED -> userId={}", settlement.getPartyLeaderId());

		} catch (Exception e) {
			log.error("푸시알림 발송 실패: settlementId={}, error={}", settlement.getSettlementId(), e.getMessage());
		}
	}

	private void sendAccountRequiredPush(Settlement settlement) {
		try {
			Party party = partyDao.findById(settlement.getPartyId()).orElse(null);
			if (party == null)
				return;

			String productName = getProductName(party.getProductId());

			Map<String, String> params = Map.of("productName", productName, "amount",
					String.valueOf(settlement.getNetAmount()));

			TemplatePushRequest pushRequest = TemplatePushRequest.builder().receiverId(settlement.getPartyLeaderId())
					.pushCode(PushCodeType.ACCOUNT_REQUIRED.getCode()).params(params)
					.moduleId(String.valueOf(settlement.getSettlementId()))
					.moduleType(PushCodeType.ACCOUNT_REQUIRED.getModuleType()).build();

			pushService.addTemplatePush(pushRequest);
			log.info("푸시알림 발송 완료: ACCOUNT_REQUIRED -> userId={}", settlement.getPartyLeaderId());

		} catch (Exception e) {
			log.error("푸시알림 발송 실패: settlementId={}, error={}", settlement.getSettlementId(), e.getMessage());
		}
	}
}