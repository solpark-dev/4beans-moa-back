package com.moa.scheduler;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.moa.common.event.SettlementCompletedEvent;
import com.moa.dao.party.PartyDao;
import com.moa.dao.settlement.SettlementDao;
import com.moa.domain.Party;
import com.moa.domain.Settlement;
import com.moa.domain.enums.SettlementStatus;
import com.moa.service.settlement.SettlementService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class SettlementScheduler {

	private final PartyDao partyDao;
	private final SettlementDao settlementDao;
	private final SettlementService settlementService;
	private final ApplicationEventPublisher eventPublisher;
	private static final int MAX_RETRY_ATTEMPTS = 3;
	private static final int RETRY_DELAY_HOURS = 2;

	@Scheduled(cron = "0 0 4 1 * *")
	public void runMonthlySettlement() {
		log.info("Starting monthly settlement scheduler...");
		LocalDate now = LocalDate.now();
		LocalDate lastMonth = now.minusMonths(1);
		String targetMonth = lastMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"));

		List<Party> activeParties = partyDao.findActiveParties();
		if (activeParties.isEmpty()) {
			log.info("No active parties found for settlement.");
			return;
		}

		for (Party party : activeParties) {
			try {
				Settlement settlement = settlementService.createMonthlySettlement(party.getPartyId(), targetMonth);

				if (settlement == null) {
					log.info("No payments to settle for partyId: {} in month: {}", party.getPartyId(), targetMonth);
					continue;
				}

				settlementService.completeSettlement(settlement.getSettlementId());
				eventPublisher.publishEvent(new SettlementCompletedEvent(party.getPartyId(), settlement.getNetAmount(),
						party.getPartyLeaderId()));

			} catch (Exception e) {
				log.error("Failed to process settlement for partyId: {}", party.getPartyId(), e);
			}
		}

		log.info("Monthly settlement scheduler finished.");
	}

	@Scheduled(cron = "0 0 * * * *")
	public void retryFailedSettlements() {
		log.info("Starting failed settlement retry scheduler...");

		List<Settlement> failedSettlements = settlementDao.findFailedSettlements();
		if (failedSettlements.isEmpty()) {
			log.info("No failed settlements to retry.");
			return;
		}

		log.info("Found {} failed settlements to retry", failedSettlements.size());

		for (Settlement settlement : failedSettlements) {
			try {
				if (settlement.getBankTranId() != null && !settlement.getBankTranId().isEmpty()) {
					log.warn("Settlement {} has bankTranId but status is FAILED. Manual intervention required.",
							settlement.getSettlementId());
					continue;
				}
				LocalDateTime createdTime = settlement.getRegDate();
				LocalDateTime now = LocalDateTime.now();
				long hoursSinceCreation = java.time.Duration.between(createdTime, now).toHours();

				if (hoursSinceCreation < RETRY_DELAY_HOURS) {
					log.debug("Settlement {} too recent to retry ({}h < {}h)", settlement.getSettlementId(),
							hoursSinceCreation, RETRY_DELAY_HOURS);
					continue;
				}
				if (hoursSinceCreation > 24) {
					log.warn("Settlement {} retry timed out ({}h > 24h). Stopping retries.",
							settlement.getSettlementId(), hoursSinceCreation);
					continue;
				}
				log.info("Retrying settlement {}", settlement.getSettlementId());
				settlementDao.updateSettlementStatus(settlement.getSettlementId(), SettlementStatus.PENDING.name(),
						null);
				settlementService.completeSettlement(settlement.getSettlementId());

				Party party = partyDao.findById(settlement.getPartyId()).orElse(null);
				if (party != null) {
					eventPublisher.publishEvent(new SettlementCompletedEvent(settlement.getPartyId(),
							settlement.getNetAmount(), settlement.getPartyLeaderId()));
				}

				log.info("Successfully retried settlement {}", settlement.getSettlementId());

			} catch (Exception e) {
				log.error("Failed to retry settlement {}: {}", settlement.getSettlementId(), e.getMessage());

			}
		}

		log.info("Failed settlement retry scheduler finished.");
	}
}
