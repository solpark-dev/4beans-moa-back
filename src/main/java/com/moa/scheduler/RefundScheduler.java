package com.moa.scheduler;

import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.moa.dao.deposit.DepositDao;
import com.moa.dao.party.PartyDao;
import com.moa.dao.product.ProductDao;
import com.moa.domain.Deposit;
import com.moa.domain.Party;
import com.moa.domain.Product;
import com.moa.domain.RefundRetryHistory;
import com.moa.domain.enums.PushCodeType;
import com.moa.dto.push.request.TemplatePushRequest;
import com.moa.service.push.PushService;
import com.moa.service.refund.RefundRetryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefundScheduler {

	private final RefundRetryService refundRetryService;
	private final PushService pushService;
	private final DepositDao depositDao;
	private final PartyDao partyDao;
	private final ProductDao productDao;

	@Scheduled(cron = "0 0 * * * *")
	public void processRefundRetries() {
		log.info("===== Refund Retry Scheduler Started =====");

		try {
			List<RefundRetryHistory> pendingRetries = refundRetryService.findPendingRetries();

			if (pendingRetries.isEmpty()) {
				log.info("No pending refund retries found");
				return;
			}

			log.info("Processing {} pending refund retries", pendingRetries.size());

			int successCount = 0;
			int failureCount = 0;

			for (RefundRetryHistory retry : pendingRetries) {
				try {
					refundRetryService.retryRefund(retry);
					successCount++;
					sendRefundSuccessPush(retry);

				} catch (Exception e) {
					log.error("Failed to process refund retry: retryId={}, depositId={}, error={}", retry.getRetryId(),
							retry.getDepositId(), e.getMessage(), e);
					failureCount++;
				}
			}

			log.info("Refund retry processing completed: success={}, failure={}", successCount, failureCount);

		} catch (Exception e) {
			log.error("Refund retry scheduler failed", e);
		} finally {
			log.info("===== Refund Retry Scheduler Finished =====");
		}
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

	private void sendRefundSuccessPush(RefundRetryHistory retry) {
		try {
			Deposit deposit = depositDao.findById(retry.getDepositId()).orElse(null);
			if (deposit == null)
				return;

			Party party = partyDao.findById(deposit.getPartyId()).orElse(null);
			if (party == null)
				return;

			String productName = getProductName(party.getProductId());

			Map<String, String> params = Map.of("productName", productName, "amount", String
					.valueOf(retry.getRefundAmount() != null ? retry.getRefundAmount() : deposit.getDepositAmount()));

			TemplatePushRequest pushRequest = TemplatePushRequest.builder().receiverId(deposit.getUserId())
					.pushCode(PushCodeType.REFUND_SUCCESS.getCode()).params(params)
					.moduleId(String.valueOf(deposit.getDepositId()))
					.moduleType(PushCodeType.REFUND_SUCCESS.getModuleType()).build();

			pushService.addTemplatePush(pushRequest);
			log.info("푸시알림 발송 완료: REFUND_SUCCESS -> userId={}", deposit.getUserId());

		} catch (Exception e) {
			log.error("푸시알림 발송 실패: retryId={}, error={}", retry.getRetryId(), e.getMessage());
		}
	}
}