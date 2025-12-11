package com.moa.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.moa.dao.party.PartyDao;
import com.moa.domain.Party;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 만료된 CLOSED 파티 정리 스케줄러
 *
 * CLOSED 상태로 30일 이상 경과한 파티를 자동으로 삭제합니다.
 * 매일 새벽 4시에 실행됩니다.
 *
 * 삭제 대상:
 * - 30분 미결제로 자동 취소된 파티 (PENDING_PAYMENT → CLOSED)
 * - 종료일이 지나 종료된 파티 (ACTIVE → CLOSED)
 * - 기타 사유로 종료된 파티
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExpiredPartyCleanupScheduler {

    private final PartyDao partyDao;

    // 보관 기간 (일)
    private static final int RETENTION_DAYS = 30;

    /**
     * 매일 새벽 4시에 실행되는 만료 파티 정리
     * CLOSED 상태로 30일 이상 경과한 파티를 삭제합니다.
     */
    @Scheduled(cron = "0 0 4 * * *") // 매일 새벽 4시 실행
    @Transactional
    public void cleanupExpiredClosedParties() {
        log.info("만료된 CLOSED 파티 정리 시작");

        try {
            // 30일 전 시간 계산
            LocalDateTime retentionThreshold = LocalDateTime.now().minusDays(RETENTION_DAYS);

            // 삭제 대상 파티 조회 (로그용)
            List<Party> expiredParties = partyDao.findExpiredClosedParties(retentionThreshold);

            if (expiredParties.isEmpty()) {
                log.info("삭제 대상 파티가 없습니다.");
                return;
            }

            log.info("삭제 대상 파티 {}개 발견 (30일 이상 경과)", expiredParties.size());

            // 각 파티의 멤버 먼저 삭제 (외래 키 제약)
            for (Party party : expiredParties) {
                try {
                    int deletedMembers = partyDao.deletePartyMembersByPartyId(party.getPartyId());
                    log.debug("파티 {} 멤버 {}명 삭제", party.getPartyId(), deletedMembers);
                } catch (Exception e) {
                    log.error("파티 {} 멤버 삭제 실패: {}", party.getPartyId(), e.getMessage());
                }
            }

            // 파티 삭제
            int deletedCount = partyDao.deleteExpiredClosedParties(retentionThreshold);

            log.info("만료된 CLOSED 파티 정리 완료: {}개 삭제됨", deletedCount);

        } catch (Exception e) {
            log.error("만료된 파티 정리 중 오류 발생: {}", e.getMessage(), e);
        }
    }
}
