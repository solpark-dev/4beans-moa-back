package com.moa.service.settlement;

import java.util.List;

import com.moa.domain.Settlement;
import com.moa.dto.settlement.response.SettlementDetailResponse;
import com.moa.dto.settlement.response.SettlementResponse;

/**
 * 정산 서비스 인터페이스
 *
 * v1.0 정산 규칙:
 * - 매월 1일 지난달 결제 내역을 집계하여 정산 생성
 * - 수수료 15% 차감 후 방장 계좌로 입금
 */
public interface SettlementService {

    /**
     * 월별 정산 생성 (스케줄러용)
     *
     * @param partyId     파티 ID
     * @param targetMonth 정산 대상 월 (YYYY-MM)
     * @return 생성된 정산 정보
     */
    Settlement createMonthlySettlement(Integer partyId, String targetMonth);

    /**
     * 정산 완료 처리 (이체 성공 후)
     *
     * @param settlementId 정산 ID
     */
    void completeSettlement(Integer settlementId);

    /**
     * 방장별 정산 내역 조회
     *
     * @param leaderId 방장 사용자 ID
     * @return 정산 목록
     */
    List<SettlementResponse> getSettlementsByLeaderId(String leaderId);

    /**
     * 정산 상세 내역 조회
     *
     * @param settlementId 정산 ID
     * @return 정산 상세 목록 (포함된 결제 내역)
     */
    List<SettlementDetailResponse> getSettlementDetails(Integer settlementId);
}
