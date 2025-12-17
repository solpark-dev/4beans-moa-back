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

@Slf4j
@Component
@RequiredArgsConstructor
public class ExpiredPartyCleanupScheduler {

	private final PartyDao partyDao;

	private static final int RETENTION_DAYS = 30;

	@Scheduled(cron = "0 0 4 * * *")
	@Transactional
	public void cleanupExpiredClosedParties() {
		log.info("만료된 CLOSED 파티 정리 시작");

		try {
			LocalDateTime retentionThreshold = LocalDateTime.now().minusDays(RETENTION_DAYS);

			List<Party> expiredParties = partyDao.findExpiredClosedParties(retentionThreshold);

			if (expiredParties.isEmpty()) {
				log.info("삭제 대상 파티가 없습니다.");
				return;
			}

			log.info("삭제 대상 파티 {}개 발견 (30일 이상 경과)", expiredParties.size());
			for (Party party : expiredParties) {
				try {
					int deletedMembers = partyDao.deletePartyMembersByPartyId(party.getPartyId());
					log.debug("파티 {} 멤버 {}명 삭제", party.getPartyId(), deletedMembers);
				} catch (Exception e) {
					log.error("파티 {} 멤버 삭제 실패: {}", party.getPartyId(), e.getMessage());
				}
			}

			int deletedCount = partyDao.deleteExpiredClosedParties(retentionThreshold);

			log.info("만료된 CLOSED 파티 정리 완료: {}개 삭제됨", deletedCount);

		} catch (Exception e) {
			log.error("만료된 파티 정리 중 오류 발생: {}", e.getMessage(), e);
		}
	}
}
