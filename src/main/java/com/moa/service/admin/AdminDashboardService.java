package com.moa.service.admin;

import com.moa.dto.admin.response.DashboardStatsResponse;

public interface AdminDashboardService {

    /**
     * 관리자 대시보드 통계 조회
     * 
     * @return 총 매출, 활성 파티 수, 전체 파티 수, 전체 유저 수
     */
    DashboardStatsResponse getDashboardStats();
}
