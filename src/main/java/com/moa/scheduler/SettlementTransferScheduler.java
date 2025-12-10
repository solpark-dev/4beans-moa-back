package com.moa.scheduler;

import com.moa.dao.account.AccountDao;
import com.moa.dao.openbanking.TransferTransactionMapper;
import com.moa.dao.settlement.SettlementDao;
import com.moa.domain.Account;
import com.moa.domain.Settlement;
import com.moa.domain.openbanking.TransferTransaction;
import com.moa.dto.openbanking.TransferDepositRequest;
import com.moa.dto.openbanking.TransferDepositResponse;
import com.moa.service.openbanking.OpenBankingClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import com.moa.domain.openbanking.TransactionStatus;

/**
 * 정산 자동 이체 스케줄러
 * 매일 오전 10시에 정산 대기 건을 조회하여 자동 이체 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementTransferScheduler {

    private final SettlementDao settlementDao;
    private final AccountDao accountDao;
    private final TransferTransactionMapper transactionMapper;
    private final OpenBankingClient openBankingClient;

    /**
     * 매일 오전 10시 정산 자동 이체 실행
     */
    @Scheduled(cron = "0 0 10 * * *")
    public void processSettlementTransfers() {
        log.info("[정산스케줄러] 자동 이체 처리 시작");

        // 정산 대기 건 조회
        List<Settlement> pendingSettlements = settlementDao.findByStatus("PENDING");

        if (pendingSettlements.isEmpty()) {
            log.info("[정산스케줄러] 처리할 정산 건이 없습니다");
            return;
        }

        log.info("[정산스케줄러] 처리 대상: {}건", pendingSettlements.size());

        int successCount = 0;
        int failCount = 0;
        int skipCount = 0;

        for (Settlement settlement : pendingSettlements) {
            try {
                boolean result = processSettlement(settlement);
                if (result) {
                    successCount++;
                } else {
                    skipCount++;
                }
            } catch (Exception e) {
                log.error("[정산스케줄러] 정산 처리 실패 - settlementId: {}", settlement.getSettlementId(), e);
                failCount++;
            }
        }

        log.info("[정산스케줄러] 처리 완료 - 성공: {}, 실패: {}, 건너뜀: {}",
                successCount, failCount, skipCount);
    }

    /**
     * 개별 정산 처리
     */
    @Transactional
    public boolean processSettlement(Settlement settlement) {
        log.info("[정산처리] 시작 - settlementId: {}, 파티장: {}, 금액: {}",
                settlement.getSettlementId(), settlement.getPartyLeaderId(), settlement.getNetAmount());

        // 파티장 계좌 조회
        Optional<Account> accountOpt = accountDao.findActiveByUserId(settlement.getPartyLeaderId());

        if (accountOpt.isEmpty()) {
            log.warn("[정산처리] 계좌 미등록 - 파티장: {}", settlement.getPartyLeaderId());
            // TODO: 파티장에게 계좌 등록 요청 알림 발송
            return false;
        }

        Account account = accountOpt.get();

        if (account.getFintechUseNum() == null || account.getFintechUseNum().isBlank()) {
            log.warn("[정산처리] 핀테크번호 없음 - 파티장: {}", settlement.getPartyLeaderId());
            return false;
        }

        // 상태를 IN_PROGRESS로 변경
        settlementDao.updateStatus(settlement.getSettlementId(), "IN_PROGRESS");

        // 입금이체 요청
        TransferDepositRequest request = TransferDepositRequest.builder()
                .fintechUseNum(account.getFintechUseNum())
                .tranAmt(settlement.getNetAmount())
                .printContent("MOA정산")
                .reqClientName("MOA")
                .build();

        TransferDepositResponse response = openBankingClient.transferDeposit(request);

        // 거래 기록 저장
        TransferTransaction transaction = TransferTransaction.builder()
                .settlementId(settlement.getSettlementId())
                .bankTranId(response.getBankTranId())
                .fintechUseNum(account.getFintechUseNum())
                .tranAmt(settlement.getNetAmount())
                .printContent("MOA정산")
                .reqClientName("MOA")
                .rspCode(response.getRspCode())
                .rspMessage(response.getRspMessage())
                .status("A0000".equals(response.getRspCode()) ? TransactionStatus.SUCCESS : TransactionStatus.FAILED)
                .build();

        transactionMapper.insert(transaction);

        // 결과에 따라 정산 상태 업데이트
        if ("A0000".equals(response.getRspCode())) {
            settlementDao.updateStatus(settlement.getSettlementId(), "COMPLETED");
            settlementDao.updateBankTranId(settlement.getSettlementId(), response.getBankTranId());
            log.info("[정산처리] 성공 - settlementId: {}, 거래ID: {}",
                    settlement.getSettlementId(), response.getBankTranId());

            // TODO: 파티장에게 정산 완료 알림 발송
            return true;
        } else {
            settlementDao.updateStatus(settlement.getSettlementId(), "FAILED");
            log.error("[정산처리] 실패 - settlementId: {}, 에러: {}",
                    settlement.getSettlementId(), response.getRspMessage());
            return false;
        }
    }

    /**
     * 수동 정산 처리 (관리자용)
     */
    @Transactional
    public boolean processSettlementManually(Integer settlementId) {
        Settlement settlement = settlementDao.findById(settlementId).orElse(null);
        if (settlement == null) {
            log.error("[수동정산] 정산 정보 없음 - settlementId: {}", settlementId);
            return false;
        }
        return processSettlement(settlement);
    }
}
