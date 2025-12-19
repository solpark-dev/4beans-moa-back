package com.moa.service.admin;

import com.moa.dto.admin.response.DashboardStatsResponse;
import com.moa.dto.admin.response.MonthlyGoalResponse;

public interface AdminDashboardService {

	DashboardStatsResponse getDashboardStats();

	MonthlyGoalResponse getMonthlyGoal();

	MonthlyGoalResponse updateMonthlyGoal(Long goalAmount);
}
