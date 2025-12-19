package com.moa.web.admin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moa.common.exception.ApiResponse;
import com.moa.dto.admin.request.MonthlyGoalRequest;
import com.moa.dto.admin.response.DashboardStatsResponse;
import com.moa.dto.admin.response.MonthlyGoalResponse;
import com.moa.service.admin.AdminDashboardService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardRestController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/stats")
    public ApiResponse<DashboardStatsResponse> getStats() {
        return ApiResponse.success(adminDashboardService.getDashboardStats());
    }

    @GetMapping("/goal")
    public ApiResponse<MonthlyGoalResponse> getMonthlyGoal() {
        return ApiResponse.success(adminDashboardService.getMonthlyGoal());
    }

    @PutMapping("/goal")
    public ApiResponse<MonthlyGoalResponse> updateMonthlyGoal(@RequestBody MonthlyGoalRequest request) {
        return ApiResponse.success(adminDashboardService.updateMonthlyGoal(request.getGoalAmount()));
    }
}
