package com.moa.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.moa.dao.party.PartyDao;
import com.moa.domain.Party;
import com.moa.service.party.PartyService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 파티 종료 스케줄러
 * 
 * 기능:
 * - 매일 새벽 3시에 종료일이 지난 파티를 자동으로 종료
 * - 파티 종료 시 모든 멤버의 보증금을 Toss Payments 결제 취소를 통해 환불
 *
 * @author MOA Team
 * @since 2025-12-08
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PartyCloseScheduler {

    private final PartyDao partyDao;
    private final PartyService partyService;

    /**
     * 매일 새벽 3시 - 종료일이 지난 파티 자동 종료
     * 
     * Cron: 0 0 3 * * * = 매일 03:00:00
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void closeExpiredParties() {
        log.info("===== Party Close Scheduler Started =====");

        try {
            LocalDateTime now = LocalDateTime.now();
            List<Party> expiredParties = partyDao.findExpiredActiveParties(now);

            if (expiredParties.isEmpty()) {
                log.info("No expired parties found");
                return;
            }

            log.info("Found {} expired parties to close", expiredParties.size());

            int successCount = 0;
            int failureCount = 0;

            for (Party party : expiredParties) {
                try {
                    // closeParty 호출 (보증금 환불 + 상태 변경)
                    partyService.closeParty(party.getPartyId(), party.getPartyLeaderId());
                    successCount++;
                    log.info("Successfully closed party: partyId={}", party.getPartyId());
                } catch (Exception e) {
                    failureCount++;
                    log.error("Failed to close party: partyId={}, error={}",
                            party.getPartyId(), e.getMessage(), e);
                    // 개별 파티 실패가 전체 프로세스를 중단시키지 않도록 계속 진행
                }
            }

            log.info("Party close processing completed: success={}, failure={}", successCount, failureCount);

        } catch (Exception e) {
            log.error("Party close scheduler failed", e);
        } finally {
            log.info("===== Party Close Scheduler Finished =====");
        }
    }
}
