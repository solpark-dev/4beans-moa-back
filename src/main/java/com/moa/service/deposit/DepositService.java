package com.moa.service.deposit;

import java.util.List;

import com.moa.domain.Deposit;
import com.moa.domain.Party;
import com.moa.dto.deposit.response.DepositResponse;
import com.moa.dto.payment.request.PaymentRequest;

/**
 * 보증금 서비스 인터페이스
 *
 * 보증금 규칙:
 * - 방장: 월구독료 전액
 * - 파티원: 인당 요금
 * 
 * 탈퇴 시 환불 정책:
 * - 파티 시작 2일 전까지: 전액 환불
 * - 파티 시작 1일 전: 50% 환불
 * - 파티 시작 후: 전액 몰수 (다음 정산에 포함)
 */
public interface DepositService {

        /**
         * 보증금 생성 (결제 처리)
         *
         * @param partyId       파티 ID
         * @param partyMemberId 파티 멤버 ID
         * @param userId        사용자 ID
         * @param amount        보증금 금액
         * @param request       결제 요청 정보
         * @return 생성된 보증금 정보
         */
        Deposit createDeposit(
                        Integer partyId,
                        Integer partyMemberId,
                        String userId,
                        Integer amount,
                        PaymentRequest request);

        /**
         * 보증금 기록 생성 (Toss 승인 없이 DB 기록만)
         * joinParty에서 사용 - 첫 달 결제와 함께 승인되었으므로 별도 승인 불필요
         *
         * @param partyId       파티 ID
         * @param partyMemberId 파티 멤버 ID
         * @param userId        사용자 ID
         * @param amount        보증금 금액
         * @param request       결제 요청 정보
         * @return 생성된 보증금 정보
         */
        Deposit createDepositWithoutConfirm(
                        Integer partyId,
                        Integer partyMemberId,
                        String userId,
                        Integer amount,
                        PaymentRequest request);

        /**
         * 보증금 상세 조회
         *
         * @param depositId 보증금 ID
         * @return 보증금 상세 정보
         */
        DepositResponse getDepositDetail(Integer depositId);

        /**
         * 사용자별 보증금 내역 조회
         *
         * @param userId 사용자 ID
         * @return 보증금 목록
         */
        List<DepositResponse> getMyDeposits(String userId);

        /**
         * 파티별 보증금 내역 조회
         *
         * @param partyId 파티 ID
         * @return 보증금 목록
         */
        List<DepositResponse> getPartyDeposits(Integer partyId);

        /**
         * 보증금 전액 환불
         *
         * @param depositId 보증금 ID
         * @param reason    환불 사유
         */
        void refundDeposit(Integer depositId, String reason);

        /**
         * 파티 탈퇴 시 보증금 처리
         * 
         * 정책:
         * - 파티 시작 2일 전까지: 전액 환불
         * - 파티 시작 1일 전: 50% 환불
         * - 파티 시작 후: 전액 몰수 → 다음 정산에 포함
         *
         * @param depositId 보증금 ID
         * @param party     파티 정보 (시작일 확인용)
         */
        void processWithdrawalRefund(Integer depositId, Party party);

        /**
         * 보증금 부분 환불
         *
         * @param depositId 보증금 ID
         * @param rate      환불 비율 (0.0 ~ 1.0)
         * @param reason    환불 사유
         */
        void partialRefundDeposit(Integer depositId, double rate, String reason);

        /**
         * 보증금 몰수 (다음 정산에 포함)
         *
         * @param depositId 보증금 ID
         * @param reason    몰수 사유
         */
        void forfeitDeposit(Integer depositId, String reason);
}
