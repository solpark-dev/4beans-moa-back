package com.moa.dao.admin;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

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
}
