package com.moa.dao.admin;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdminDashboardDao {

    // 기본 통계
    long getTotalRevenue();
    long getActivePartyCount();
    long getTotalPartyCount();
    long getTotalUserCount();

    // 추가 통계
    long getTodayNewUsers();
    long getThisMonthRevenue();
    long getRecruitingPartyCount();
    long getPendingPaymentCount();
    long getCompletedPaymentCount();
    long getThisMonthPaymentCount();

    // OTT별 파티 통계
    List<Map<String, Object>> getOttPartyStats();

    // 최근 7일 매출
    List<Map<String, Object>> getDailyRevenues();

    // 최근 가입 회원 (5명)
    List<Map<String, Object>> getRecentUsers();

    // 최근 결제 내역 (5건)
    List<Map<String, Object>> getRecentPayments();
}
