package com.moa.scheduler;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.moa.dao.party.PartyDao;
import com.moa.dao.partymember.PartyMemberDao;
import com.moa.dao.product.ProductDao;
import com.moa.domain.Party;
import com.moa.domain.PartyMember;
import com.moa.domain.PaymentRetryHistory;
import com.moa.domain.Product;
import com.moa.domain.enums.PushCodeType;
import com.moa.dto.push.request.TemplatePushRequest;
import com.moa.service.payment.PaymentRetryService;
import com.moa.service.payment.PaymentService;
import com.moa.service.push.PushService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentScheduler {

	private final PartyDao partyDao;
	private final PartyMemberDao partyMemberDao;
	private final PaymentService paymentService;
	private final PaymentRetryService retryService;
	private final PushService pushService;
	private final ProductDao productDao;

	@Scheduled(cron = "0 0 2 * * *")
	public void runDailyPayments() {
		log.info("Starting daily payment scheduler...");

		LocalDate today = LocalDate.now();
		String targetMonth = today.format(DateTimeFormatter.ofPattern("yyyy-MM"));

		processNewMonthlyPayments(today, targetMonth);
		processRetryPayments(today, targetMonth);

		log.info("Daily payment scheduler finished.");
	}

	private void processNewMonthlyPayments(LocalDate today, String targetMonth) {
		int currentDay = today.getDayOfMonth();
		int lastDayOfMonth = today.lengthOfMonth();

		log.info("Processing new monthly payments for day {} (last day: {})", currentDay, lastDayOfMonth);

		List<Party> parties = partyDao.findPartiesByPaymentDay(currentDay, lastDayOfMonth);
		log.info("Found {} parties for payment on day {}", parties.size(), currentDay);

		for (Party party : parties) {
			try {
				processPartyPayments(party, targetMonth);
			} catch (Exception e) {
				log.error("Failed to process payments for partyId: {}", party.getPartyId(), e);
			}
		}
	}

	private void processPartyPayments(Party party, String targetMonth) {
		if (!isPartyPaymentEligible(party)) {
			log.warn("결제 불가능한 파티 상태, 스킵: partyId={}, status={}", party.getPartyId(), party.getPartyStatus());
			return;
		}
		Party freshParty = partyDao.findById(party.getPartyId()).orElse(null);
		if (freshParty == null || !isPartyPaymentEligible(freshParty)) {
			log.warn("결제 처리 중 파티 상태 변경 감지, 스킵: partyId={}", party.getPartyId());
			return;
		}

		List<PartyMember> members = partyMemberDao.findActiveMembersExcludingLeader(party.getPartyId());

		log.info("Processing {} active members for partyId: {}", members.size(), party.getPartyId());

		for (PartyMember member : members) {
			try {
				paymentService.processMonthlyPayment(party.getPartyId(), member.getPartyMemberId(), member.getUserId(),
						party.getMonthlyFee(), targetMonth);
			} catch (Exception e) {
				log.error("Failed to process payment for partyMemberId: {}", member.getPartyMemberId(), e);
			}
		}
	}

	private boolean isPartyPaymentEligible(Party party) {
		if (party == null) {
			return false;
		}

		return switch (party.getPartyStatus()) {
		case ACTIVE -> true;
		case RECRUITING -> false;
		case PENDING_PAYMENT -> false;
		case SUSPENDED -> false;
		case DISBANDED -> false;
		case CLOSED -> false;
		};
	}

	private void processRetryPayments(LocalDate today, String targetMonth) {
		List<PaymentRetryHistory> retries = retryService.findPendingRetries(today);
		log.info("Found {} payments pending retry", retries.size());

		for (PaymentRetryHistory retry : retries) {
			try {
				retryService.retryPayment(retry, targetMonth);
			} catch (Exception e) {
				log.error("Failed to retry paymentId: {}", retry.getPaymentId(), e);
			}
		}
	}

	@Scheduled(cron = "0 0 18 * * *")
	public void sendPaymentUpcomingNotifications() {
		log.info("===== Payment Upcoming Notification Started =====");

		try {
			LocalDate tomorrow = LocalDate.now().plusDays(1);
			int tomorrowDay = tomorrow.getDayOfMonth();
			int lastDayOfMonth = tomorrow.lengthOfMonth();

			List<Party> parties = partyDao.findPartiesByPaymentDay(tomorrowDay, lastDayOfMonth);

			if (parties.isEmpty()) {
				log.info("내일 결제 예정인 파티가 없습니다.");
				return;
			}

			log.info("내일 결제 예정 파티 {}개 발견", parties.size());

			int successCount = 0;
			int failCount = 0;

			for (Party party : parties) {
				try {
					List<PartyMember> members = partyMemberDao.findActiveMembersExcludingLeader(party.getPartyId());

					for (PartyMember member : members) {
						try {
							sendPaymentUpcomingPush(party, member);
							successCount++;
						} catch (Exception e) {
							log.error("푸시 발송 실패: userId={}, error={}", member.getUserId(), e.getMessage());
							failCount++;
						}
					}
				} catch (Exception e) {
					log.error("파티 처리 실패: partyId={}, error={}", party.getPartyId(), e.getMessage());
				}
			}

			log.info("결제 예정 알림 발송 완료: 성공={}, 실패={}", successCount, failCount);

		} catch (Exception e) {
			log.error("결제 예정 알림 스케줄러 실패", e);
		} finally {
			log.info("===== Payment Upcoming Notification Finished =====");
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

	private void sendPaymentUpcomingPush(Party party, PartyMember member) {
		String productName = getProductName(party.getProductId());
		LocalDate tomorrow = LocalDate.now().plusDays(1);

		Map<String, String> params = Map.of("productName", productName, "amount", String.valueOf(party.getMonthlyFee()),
				"paymentDate", tomorrow.format(DateTimeFormatter.ofPattern("M월 d일")));

		TemplatePushRequest pushRequest = TemplatePushRequest.builder().receiverId(member.getUserId())
				.pushCode(PushCodeType.PAY_UPCOMING.getCode()).params(params)
				.moduleId(String.valueOf(party.getPartyId())).moduleType(PushCodeType.PAY_UPCOMING.getModuleType())
				.build();

		pushService.addTemplatePush(pushRequest);
		log.info("푸시알림 발송 완료: PAY_UPCOMING -> userId={}", member.getUserId());
	}
}