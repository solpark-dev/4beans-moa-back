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

    // 재시도 설정
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final int RETRY_DELAY_HOURS = 2; // 재시도 간격 (시간)

    /**
     * 월별 정산 스케줄러
     * 매월 1일 새벽 4시에 실행
     * 전월 사용분에 대한 정산을 생성하고 즉시 이체 처리 (Full Automation)
     */
    @Scheduled(cron = "0 0 4 1 * *")
    public void runMonthlySettlement() {
        log.info("Starting monthly settlement scheduler...");

        // 1. 정산 대상 월 계산 (이번 달 1일 실행 -> 지난 달이 대상)
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
                // 1. 정산 생성
                Settlement settlement = settlementService.createMonthlySettlement(party.getPartyId(), targetMonth);

                // 정산할 내역이 없으면 스킵
                if (settlement == null) {
                    log.info("No payments to settle for partyId: {} in month: {}", party.getPartyId(), targetMonth);
                    continue;
                }

                // 2. 정산 완료 (이체)
                settlementService.completeSettlement(settlement.getSettlementId());

                // 3. 이벤트 발행
                eventPublisher.publishEvent(new SettlementCompletedEvent(
                        party.getPartyId(),
                        settlement.getNetAmount(), // netAmount 사용
                        party.getPartyLeaderId()));

            } catch (Exception e) {
                log.error("Failed to process settlement for partyId: {}", party.getPartyId(), e);
                // 개별 파티 실패가 전체 프로세스를 중단시키지 않도록 예외 처리
            }
        }

        log.info("Monthly settlement scheduler finished.");
    }

    /**
     * 실패한 정산 재시도 스케줄러
     * 매시간 정각에 실행
     */
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
                // 재시도 가능 여부 확인
                // 1. 이미 bankTranId가 있으면 API 호출은 성공했으나 DB 업데이트 실패한 것 → 재시도 불가
                if (settlement.getBankTranId() != null && !settlement.getBankTranId().isEmpty()) {
                    log.warn("Settlement {} has bankTranId but status is FAILED. Manual intervention required.",
                            settlement.getSettlementId());
                    continue;
                }

                // 2. 생성 후 최소 재시도 간격 확인 (지수 백오프)
                LocalDateTime createdTime = settlement.getRegDate();
                LocalDateTime now = LocalDateTime.now();
                long hoursSinceCreation = java.time.Duration.between(createdTime, now).toHours();

                if (hoursSinceCreation < RETRY_DELAY_HOURS) {
                    log.debug("Settlement {} too recent to retry ({}h < {}h)",
                            settlement.getSettlementId(), hoursSinceCreation, RETRY_DELAY_HOURS);
                    continue;
                }

                // 24시간 지난 건은 더 이상 재시도하지 않음 (무한 루프 방지)
                if (hoursSinceCreation > 24) {
                    log.warn("Settlement {} retry timed out ({}h > 24h). Stopping retries.",
                            settlement.getSettlementId(), hoursSinceCreation);
                    continue;
                }

                // 3. 재시도 실행
                log.info("Retrying settlement {}", settlement.getSettlementId());

                // 상태를 PENDING으로 변경하여 재시도 가능하게 함
                settlementDao.updateSettlementStatus(settlement.getSettlementId(),
                        SettlementStatus.PENDING.name(), null);

                // 정산 완료 시도
                settlementService.completeSettlement(settlement.getSettlementId());

                // 파티 정보 조회하여 이벤트 발행
                Party party = partyDao.findById(settlement.getPartyId()).orElse(null);
                if (party != null) {
                    eventPublisher.publishEvent(new SettlementCompletedEvent(
                            settlement.getPartyId(),
                            settlement.getNetAmount(),
                            settlement.getPartyLeaderId()));
                }

                log.info("Successfully retried settlement {}", settlement.getSettlementId());

            } catch (Exception e) {
                log.error("Failed to retry settlement {}: {}", settlement.getSettlementId(), e.getMessage());
                // 실패 시 상태는 이미 FAILED로 저장되어 있음
            }
        }

        log.info("Failed settlement retry scheduler finished.");
    }
}
