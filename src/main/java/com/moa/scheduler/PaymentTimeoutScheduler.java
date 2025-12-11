package com.moa.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.moa.dao.party.PartyDao;
import com.moa.domain.Party;
import com.moa.domain.enums.PartyStatus;
import com.moa.service.party.PartyService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 결제 타임아웃 스케줄러
 * 
 * PENDING_PAYMENT 상태로 30분 이상 경과한 파티를 자동으로 취소합니다.
 * 5분마다 실행됩니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentTimeoutScheduler {

    private final PartyDao partyDao;
    private final PartyService partyService;

    // 타임아웃 시간 (분)
    private static final int TIMEOUT_MINUTES = 30;

    /**
     * 5분마다 실행되는 결제 타임아웃 체크
     * PENDING_PAYMENT 상태로 30분 이상 경과한 파티를 취소합니다.
     */
    @Scheduled(fixedRate = 5 * 60 * 1000) // 5분마다 실행
    public void checkPaymentTimeout() {
        log.info("결제 타임아웃 체크 시작");

        try {
            // 30분 전 시간 계산
            LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(TIMEOUT_MINUTES);

            // PENDING_PAYMENT 상태이고 생성 시간이 30분 이상 경과한 파티 조회
            List<Party> expiredParties = partyDao.findExpiredPendingPaymentParties(
                    PartyStatus.PENDING_PAYMENT, 
                    timeoutThreshold
            );

            if (expiredParties.isEmpty()) {
                log.info("타임아웃된 파티가 없습니다.");
                return;
            }

            log.info("타임아웃된 파티 {}개 발견", expiredParties.size());

            // 각 파티 취소 처리
            for (Party party : expiredParties) {
                try {
                    partyService.cancelExpiredParty(
                            party.getPartyId(),
                            "결제 타임아웃 (30분 초과)"
                    );
                    log.info("파티 취소 완료: partyId={}", party.getPartyId());
                } catch (Exception e) {
                    log.error("파티 취소 실패: partyId={}, error={}", 
                            party.getPartyId(), e.getMessage());
                }
            }

            log.info("결제 타임아웃 체크 완료: 처리된 파티 {}개", expiredParties.size());

        } catch (Exception e) {
            log.error("결제 타임아웃 체크 중 오류 발생: {}", e.getMessage(), e);
        }
    }
}
