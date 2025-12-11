package com.moa.service.admin.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moa.dao.admin.AdminDashboardDao;
import com.moa.dto.admin.response.DashboardStatsResponse;
import com.moa.service.admin.AdminDashboardService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final AdminDashboardDao adminDashboardDao;

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        long totalRevenue = adminDashboardDao.getTotalRevenue();
        long activePartyCount = adminDashboardDao.getActivePartyCount();
        long totalPartyCount = adminDashboardDao.getTotalPartyCount();
        long totalUserCount = adminDashboardDao.getTotalUserCount();

        return DashboardStatsResponse.builder()
                .totalRevenue(totalRevenue)
                .activePartyCount(activePartyCount)
                .totalPartyCount(totalPartyCount)
                .totalUserCount(totalUserCount)
                .build();
    }
}
