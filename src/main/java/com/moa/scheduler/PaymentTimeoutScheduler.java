package com.moa.scheduler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.moa.dao.party.PartyDao;
import com.moa.dao.product.ProductDao;
import com.moa.domain.Party;
import com.moa.domain.Product;
import com.moa.domain.enums.PartyStatus;
import com.moa.domain.enums.PushCodeType;
import com.moa.dto.push.request.TemplatePushRequest;
import com.moa.service.party.PartyService;
import com.moa.service.push.PushService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentTimeoutScheduler {

	private final PartyDao partyDao;
	private final PartyService partyService;
	private final PushService pushService;
	private final ProductDao productDao;
	private static final int TIMEOUT_MINUTES = 30;

	@Scheduled(fixedRate = 5 * 60 * 1000)
	public void checkPaymentTimeout() {
		log.info("결제 타임아웃 체크 시작");

		try {
			LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(TIMEOUT_MINUTES);

			List<Party> expiredParties = partyDao.findExpiredPendingPaymentParties(PartyStatus.PENDING_PAYMENT,
					timeoutThreshold);

			if (expiredParties.isEmpty()) {
				log.info("타임아웃된 파티가 없습니다.");
				return;
			}

			log.info("타임아웃된 파티 {}개 발견", expiredParties.size());

			for (Party party : expiredParties) {
				try {
					partyService.cancelExpiredParty(party.getPartyId(), "결제 타임아웃 (30분 초과)");
					log.info("파티 취소 완료: partyId={}", party.getPartyId());

					sendPaymentTimeoutPush(party);

				} catch (Exception e) {
					log.error("파티 취소 실패: partyId={}, error={}", party.getPartyId(), e.getMessage());
				}
			}

			log.info("결제 타임아웃 체크 완료: 처리된 파티 {}개", expiredParties.size());

		} catch (Exception e) {
			log.error("결제 타임아웃 체크 중 오류 발생: {}", e.getMessage(), e);
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

	private void sendPaymentTimeoutPush(Party party) {
		try {
			String productName = getProductName(party.getProductId());

			Map<String, String> params = Map.of("productName", productName, "timeoutMinutes",
					String.valueOf(TIMEOUT_MINUTES));

			TemplatePushRequest pushRequest = TemplatePushRequest.builder().receiverId(party.getPartyLeaderId())
					.pushCode(PushCodeType.PAY_TIMEOUT.getCode()).params(params)
					.moduleId(String.valueOf(party.getPartyId())).moduleType(PushCodeType.PAY_TIMEOUT.getModuleType())
					.build();

			pushService.addTemplatePush(pushRequest);
			log.info("푸시알림 발송 완료: PAY_TIMEOUT -> userId={}", party.getPartyLeaderId());

		} catch (Exception e) {
			log.error("푸시알림 발송 실패: partyId={}, error={}", party.getPartyId(), e.getMessage());
		}
	}

}