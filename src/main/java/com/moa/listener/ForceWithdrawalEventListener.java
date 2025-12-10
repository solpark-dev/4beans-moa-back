package com.moa.listener;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.moa.common.event.MonthlyPaymentFailedEvent;
import com.moa.dao.partymember.PartyMemberDao;
import com.moa.dao.party.PartyDao;
import com.moa.domain.Party;
import com.moa.domain.PartyMember;
import com.moa.domain.enums.PartyStatus;
import com.moa.service.deposit.DepositService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 월 결제 실패 이벤트 리스너
 * 
 * 4회 결제 실패 시:
 * 1. 보증금 몰수 (FORFEITED)
 * 2. 파티원 강제 탈퇴 (WITHDRAWN)
 * 3. 파티 인원 감소
 * 4. 파티 상태 변경 (ACTIVE → RECRUITING)
 *
 * @author MOA Team
 * @since 2025-12-08
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ForceWithdrawalEventListener {

    private final PartyMemberDao partyMemberDao;
    private final PartyDao partyDao;
    private final DepositService depositService;

    /**
     * 월 결제 최종 실패 이벤트 처리
     * MonthlyPaymentFailedEvent는 4회 결제 실패 시 발행됨
     * 
     * @param event 결제 실패 이벤트
     */
    @EventListener
    @Transactional
    public void handlePaymentFinalFailure(MonthlyPaymentFailedEvent event) {
        log.warn("===== 강제 탈퇴 처리 시작 =====");
        log.warn("partyId={}, partyMemberId={}, userId={}, errorMessage={}",
                event.getPartyId(), event.getPartyMemberId(),
                event.getUserId(), event.getErrorMessage());

        try {
            // 1. 파티원 조회
            PartyMember member = partyMemberDao
                    .findByPartyIdAndUserId(event.getPartyId(), event.getUserId())
                    .orElse(null);

            if (member == null) {
                log.warn("파티원을 찾을 수 없음: partyId={}, userId={}",
                        event.getPartyId(), event.getUserId());
                return;
            }

            // 2. 보증금 몰수
            com.moa.domain.Deposit deposit = depositService.findByPartyIdAndUserId(event.getPartyId(),
                    event.getUserId());
            if (deposit != null) {
                depositService.forfeitDeposit(deposit.getDepositId(), "4회 결제 실패로 인한 강제 탈퇴");
                log.info("보증금 몰수 완료: depositId={}", deposit.getDepositId());
            } else {
                log.warn("보증금 정보 없음: partyMemberId={}", member.getPartyMemberId());
            }

            // 3. 파티원 상태 → WITHDRAWN + 탈퇴일 기록
            // leaveParty 메서드가 MEMBER_STATUS='WITHDRAWN', WITHDRAW_DATE=NOW() 처리
            partyMemberDao.leaveParty(member.getPartyMemberId());
            log.info("파티원 탈퇴 처리 완료: partyMemberId={}", member.getPartyMemberId());

            // 4. 파티 인원 감소
            int updatedRows = partyDao.decrementCurrentMembers(event.getPartyId());
            if (updatedRows > 0) {
                log.info("파티 인원 감소 완료: partyId={}", event.getPartyId());
            }

            // 5. 파티 상태 변경 (ACTIVE → RECRUITING)
            Party party = partyDao.findById(event.getPartyId()).orElse(null);
            if (party != null && party.getPartyStatus() == PartyStatus.ACTIVE) {
                // 현재 인원이 최대 인원보다 적으면 RECRUITING으로 변경
                if (party.getCurrentMembers() < party.getMaxMembers()) {
                    partyDao.updatePartyStatus(event.getPartyId(), PartyStatus.RECRUITING);
                    log.info("파티 상태 변경: ACTIVE → RECRUITING, partyId={}", event.getPartyId());
                }
            }

            log.info("===== 강제 탈퇴 완료: partyMemberId={}, userId={} =====",
                    member.getPartyMemberId(), event.getUserId());

        } catch (Exception e) {
            log.error("강제 탈퇴 처리 실패: partyId={}, userId={}",
                    event.getPartyId(), event.getUserId(), e);
            // 예외를 던지면 트랜잭션 롤백됨
            // 하지만 여기서는 로그만 남기고 계속 진행 (다른 처리에 영향 주지 않기 위해)
        }
    }
}
