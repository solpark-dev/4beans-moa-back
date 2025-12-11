package com.moa.dto.admin.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardStatsResponse {
    private long totalRevenue; // 총 매출 (모든 정산 완료된 금액의 수수료 합계 or 전체 결제 금액)
    private long activePartyCount; // 현재 활성화된 파티 수
    private long totalPartyCount; // 누적 파티 수
    private long totalUserCount; // 총 회원 수
}
