package com.moa.dao.admin;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdminDashboardDao {

	long getTotalRevenue();

	long getActivePartyCount();

	long getTotalPartyCount();

	long getTotalUserCount();

	long getTodayNewUsers();

	long getThisMonthRevenue();

	long getRecruitingPartyCount();

	long getPendingPaymentCount();

	long getCompletedPaymentCount();

	long getThisMonthPaymentCount();

	List<Map<String, Object>> getOttPartyStats();

	List<Map<String, Object>> getDailyRevenues();

	List<Map<String, Object>> getRecentUsers();

	List<Map<String, Object>> getRecentPayments();

	// Phase 1: 월별 매출
	List<Map<String, Object>> getMonthlyRevenues();

	// Phase 2: 증감률 계산용
	long getLastMonthRevenue();
	long getLastMonthUserCount();
	long getYesterdayNewUsers();

	// Phase 3: 주간 사용자 추이
	List<Map<String, Object>> getWeeklyNewUsers();
	List<Map<String, Object>> getWeeklyActiveUsers();

	// Phase 4: 실시간 알림
	List<Map<String, Object>> getRecentFailedPayments();
	List<Map<String, Object>> getExpiringParties();

	// 월 목표 관리
	Long getMonthlyGoal(@Param("targetMonth") String targetMonth);
	void insertMonthlyGoal(@Param("targetMonth") String targetMonth, @Param("goalAmount") Long goalAmount);
	void updateMonthlyGoal(@Param("targetMonth") String targetMonth, @Param("goalAmount") Long goalAmount);
}
