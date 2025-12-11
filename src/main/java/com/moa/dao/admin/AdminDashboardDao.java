package com.moa.dao.admin;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdminDashboardDao {

    // 통계 데이터 조회
    // 1. 총 매출 (COMPLETED 된 PAYMENT의 합계 또는 정산 수수료 합계 - 여기선 간단히 PAYMENT 총액으로 가정하거나
    // 요구사항에 맞춤)
    // 2. 활성 파티 수
    // 3. 전체 파티 수
    // 4. 전체 유저 수

    long getTotalRevenue();

    long getActivePartyCount();

    long getTotalPartyCount();

    long getTotalUserCount();
}
