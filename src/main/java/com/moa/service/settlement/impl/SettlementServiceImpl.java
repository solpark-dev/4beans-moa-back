package com.moa.service.settlement.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moa.common.exception.BusinessException;
import com.moa.common.exception.ErrorCode;
import com.moa.dao.account.AccountDao;
import com.moa.dao.deposit.DepositDao;
import com.moa.dao.party.PartyDao;
import com.moa.dao.payment.PaymentDao;
import com.moa.dao.settlement.SettlementDao;
import com.moa.dao.settlement.SettlementDetailDao;
import com.moa.domain.Account;
import com.moa.domain.Deposit;
import com.moa.domain.Party;
import com.moa.domain.Settlement;
import com.moa.domain.SettlementDetail;
import com.moa.domain.enums.SettlementStatus;
import com.moa.dto.payment.response.PaymentResponse;
import com.moa.dto.settlement.response.SettlementDetailResponse;
import com.moa.dto.settlement.response.SettlementResponse;
import com.moa.service.settlement.SettlementService;

import com.moa.service.openbanking.OpenBankingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SettlementServiceImpl implements SettlementService {

        private final SettlementDao settlementDao;
        private final SettlementDetailDao settlementDetailDao;
        private final PaymentDao paymentDao;
        private final PartyDao partyDao;
        private final AccountDao accountDao;
        private final DepositDao depositDao;
        private final OpenBankingService openBankingService;

        // 수수료율 (15%)
        private static final double COMMISSION_RATE = 0.15;

        @Override
        public Settlement createMonthlySettlement(Integer partyId, String targetMonth) {
                // 1. 이미 정산되었는지 확인
                if (settlementDao.findByPartyIdAndMonth(partyId, targetMonth).isPresent()) {
                        throw new BusinessException(ErrorCode.DUPLICATE_SETTLEMENT);
                }

                // 2. 파티 정보 조회 (방장 ID 및 시작일 확인용)
                Party party = partyDao.findById(partyId)
                                .orElseThrow(() -> new BusinessException(ErrorCode.PARTY_NOT_FOUND));

                // 3. 방장 계좌 정보 조회
                Account account = accountDao.findByUserId(party.getPartyLeaderId())
                                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

                // 4. 파티 시작일 기준 정산 기간 계산
                LocalDateTime partyStartDate = party.getStartDate();
                if (partyStartDate == null) {
                        throw new BusinessException(ErrorCode.START_DATE_REQUIRED);
                }

                // 정산 기간: 파티 시작일의 day를 기준으로 이전 달 billing cycle 계산
                // 예: 1월 15일 시작 -> 2월 1일 정산 시 1월 15일 ~ 1월 31일
                // 3월 1일 정산 시 2월 15일 ~ 3월 14일
                int billingDay = partyStartDate.getDayOfMonth();
                LocalDateTime tempStartDate;
                LocalDateTime tempEndDate;

                // targetMonth 파싱 (yyyy-MM)
                String[] monthParts = targetMonth.split("-");
                int targetYear = Integer.parseInt(monthParts[0]);
                int targetMonthNum = Integer.parseInt(monthParts[1]);

                // 정산 시작일: targetMonth의 billingDay
                // 정산 종료일: 다음달의 (billingDay - 1)일
                if (billingDay == 1) {
                        // 1일 시작인 경우: 전월 1일 ~ 전월 말일
                        tempStartDate = LocalDateTime.of(targetYear, targetMonthNum, 1, 0, 0, 0);
                        tempEndDate = tempStartDate.plusMonths(1).minusDays(1).withHour(23).withMinute(59)
                                        .withSecond(59);
                } else {
                        // billingDay가 2~31인 경우: 전월 billingDay ~ 이번달 (billingDay - 1)
                        tempStartDate = LocalDateTime.of(targetYear, targetMonthNum, billingDay, 0, 0, 0);
                        tempEndDate = tempStartDate.plusMonths(1).minusDays(1).withHour(23).withMinute(59)
                                        .withSecond(59);
                }

                // 파티 시작일 이전은 정산하지 않음 (final 변수로 선언)
                final LocalDateTime settlementStartDate = tempStartDate.isBefore(partyStartDate) ? partyStartDate
                                : tempStartDate;
                final LocalDateTime settlementEndDate = tempEndDate;

                // 5. 해당 기간의 결제 내역 조회 (COMPLETED 상태이고 날짜 범위 내)
                List<PaymentResponse> payments = paymentDao.findByPartyId(partyId);

                List<PaymentResponse> targetPayments = payments.stream()
                                .filter(p -> "COMPLETED".equals(p.getPaymentStatus()))
                                .filter(p -> p.getPaymentDate() != null)
                                .filter(p -> !p.getPaymentDate().isBefore(settlementStartDate)
                                                && !p.getPaymentDate().isAfter(settlementEndDate))
                                .collect(Collectors.toList());

                // 6. 해당 기간에 몰수된 보증금 조회
                List<Deposit> forfeitedDeposits = depositDao.findForfeitedByPartyIdAndPeriod(
                                partyId, settlementStartDate, settlementEndDate);

                // 몰수 보증금 총액
                int forfeitedAmount = forfeitedDeposits.stream()
                                .mapToInt(Deposit::getDepositAmount)
                                .sum();

                if (targetPayments.isEmpty() && forfeitedDeposits.isEmpty()) {
                        return null; // 정산할 내역 없음
                }

                // 7. 총액 계산 (결제 + 몰수 보증금)
                int paymentTotal = targetPayments.stream()
                                .mapToInt(PaymentResponse::getPaymentAmount)
                                .sum();
                int totalAmount = paymentTotal + forfeitedAmount;

                // 8. 수수료 및 순 정산액 계산 (몰수 보증금은 수수료 없이 전액 지급)
                int commissionAmount = (int) (paymentTotal * COMMISSION_RATE);
                int netAmount = totalAmount - commissionAmount;

                // 8. Settlement 생성
                Settlement settlement = Settlement.builder()
                                .partyId(partyId)
                                .partyLeaderId(party.getPartyLeaderId())
                                .accountId(account.getAccountId())
                                .settlementMonth(targetMonth)
                                .settlementType("MONTHLY")
                                .totalAmount(totalAmount)
                                .commissionRate(COMMISSION_RATE)
                                .commissionAmount(commissionAmount)
                                .netAmount(netAmount)
                                .settlementStatus(SettlementStatus.PENDING)
                                .regDate(LocalDateTime.now())
                                .build();

                settlementDao.insertSettlement(settlement);

                // 9. SettlementDetail 생성
                for (PaymentResponse p : targetPayments) {
                        // N+1 문제 해결: PaymentResponse에 있는 정보 사용
                        SettlementDetail detail = SettlementDetail.builder()
                                        .settlementId(settlement.getSettlementId())
                                        .paymentId(p.getPaymentId())
                                        .partyMemberId(p.getPartyMemberId())
                                        .userId(p.getUserId())
                                        .paymentAmount(p.getPaymentAmount())
                                        .regDate(LocalDateTime.now())
                                        .build();

                        settlementDetailDao.insertSettlementDetail(detail);
                }

                return settlement;
        }

        @Override
        public void completeSettlement(Integer settlementId) {
                // 1. 정산 정보 조회
                Settlement settlement = settlementDao.findById(settlementId)
                                .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));

                // 2. 상태 확인 (이미 완료되었거나 처리 중인 경우)
                if (settlement.getSettlementStatus() == SettlementStatus.COMPLETED) {
                        throw new BusinessException(ErrorCode.SETTLEMENT_ALREADY_COMPLETED);
                }
                if (settlement.getSettlementStatus() == SettlementStatus.IN_PROGRESS) {
                        throw new BusinessException(ErrorCode.SETTLEMENT_FAILED); // 처리 중인 건은 재시도 불가
                }

                // 3. 계좌 정보 조회
                Account account = accountDao.findById(settlement.getAccountId())
                                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

                // 4. 계좌 인증 여부 확인
                if (!"Y".equals(account.getIsVerified())) {
                        throw new BusinessException(ErrorCode.ACCOUNT_NOT_VERIFIED);
                }

                String bankTranId = null;
                try {
                        // 5. 상태를 IN_PROGRESS로 변경 (중복 처리 방지)
                        settlementDao.updateSettlementStatus(settlementId, SettlementStatus.IN_PROGRESS.name(), null);
                        settlement.setSettlementStatus(SettlementStatus.IN_PROGRESS);

                        // 6. 오픈뱅킹 입금이체 요청
                        bankTranId = openBankingService.depositToUser(
                                        account.getBankCode(),
                                        account.getAccountNumber(),
                                        account.getAccountHolder(),
                                        settlement.getNetAmount());

                        // 7. 정산 상태를 COMPLETED로 변경 및 거래번호 저장
                        settlementDao.updateSettlementStatus(settlementId, SettlementStatus.COMPLETED.name(),
                                        bankTranId);
                        settlement.setSettlementStatus(SettlementStatus.COMPLETED);
                        settlement.setBankTranId(bankTranId);

                } catch (Exception e) {
                        // 8. 실패 시 상태를 FAILED로 변경
                        // API 호출 성공 후 DB 저장 실패한 경우 bankTranId를 저장하여 이중 지급 방지
                        String finalBankTranId = bankTranId != null ? bankTranId : null;
                        settlementDao.updateSettlementStatus(settlementId, SettlementStatus.FAILED.name(),
                                        finalBankTranId);
                        settlement.setSettlementStatus(SettlementStatus.FAILED);

                        throw new BusinessException(ErrorCode.SETTLEMENT_FAILED);
                }
        }

        @Override
        @Transactional(readOnly = true)
        public List<SettlementResponse> getSettlementsByLeaderId(String leaderId) {
                return settlementDao.findByLeaderId(leaderId);
        }

        @Override
        @Transactional(readOnly = true)
        public List<SettlementDetailResponse> getSettlementDetails(Integer settlementId) {
                return settlementDetailDao.findBySettlementId(settlementId);
        }
}
