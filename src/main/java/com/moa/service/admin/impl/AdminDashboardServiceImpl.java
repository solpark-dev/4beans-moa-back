package com.moa.service.admin.impl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moa.dao.admin.AdminDashboardDao;
import com.moa.dto.admin.response.DashboardStatsResponse;
import com.moa.dto.admin.response.DashboardStatsResponse.DailyRevenue;
import com.moa.dto.admin.response.DashboardStatsResponse.OttPartyStats;
import com.moa.dto.admin.response.DashboardStatsResponse.RecentPayment;
import com.moa.dto.admin.response.DashboardStatsResponse.RecentUser;
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

        long todayNewUsers = adminDashboardDao.getTodayNewUsers();
        long thisMonthRevenue = adminDashboardDao.getThisMonthRevenue();
        long recruitingPartyCount = adminDashboardDao.getRecruitingPartyCount();
        long pendingPaymentCount = adminDashboardDao.getPendingPaymentCount();
        long completedPaymentCount = adminDashboardDao.getCompletedPaymentCount();
        long thisMonthPaymentCount = adminDashboardDao.getThisMonthPaymentCount();

        List<OttPartyStats> ottPartyStats = adminDashboardDao.getOttPartyStats().stream()
                .map(m -> OttPartyStats.builder()
                        .ottName((String) m.get("ottName"))
                        .partyCount(toLong(m.get("partyCount")))
                        .activeCount(toLong(m.get("activeCount")))
                        .build())
                .collect(Collectors.toList());

        List<DailyRevenue> dailyRevenues = adminDashboardDao.getDailyRevenues().stream()
                .map(m -> DailyRevenue.builder()
                        .date((String) m.get("date"))
                        .amount(toLong(m.get("amount")))
                        .build())
                .collect(Collectors.toList());

        List<RecentUser> recentUsers = adminDashboardDao.getRecentUsers().stream()
                .map(m -> RecentUser.builder()
                        .odUserId((String) m.get("odUserId"))
                        .userName((String) m.get("userName"))
                        .userEmail((String) m.get("userEmail"))
                        .regDate((String) m.get("regDate"))
                        .build())
                .collect(Collectors.toList());

        List<RecentPayment> recentPayments = adminDashboardDao.getRecentPayments().stream()
                .map(m -> RecentPayment.builder()
                        .paymentId(toLong(m.get("paymentId")))
                        .odUserId((String) m.get("odUserId"))
                        .amount(toLong(m.get("amount")))
                        .status((String) m.get("status"))
                        .paymentDate((String) m.get("paymentDate"))
                        .partyName((String) m.get("partyName"))
                        .build())
                .collect(Collectors.toList());

        return DashboardStatsResponse.builder()
                .totalRevenue(totalRevenue)
                .activePartyCount(activePartyCount)
                .totalPartyCount(totalPartyCount)
                .totalUserCount(totalUserCount)
                .todayNewUsers(todayNewUsers)
                .thisMonthRevenue(thisMonthRevenue)
                .recruitingPartyCount(recruitingPartyCount)
                .pendingPaymentCount(pendingPaymentCount)
                .completedPaymentCount(completedPaymentCount)
                .thisMonthPaymentCount(thisMonthPaymentCount)
                .ottPartyStats(ottPartyStats)
                .dailyRevenues(dailyRevenues)
                .recentUsers(recentUsers)
                .recentPayments(recentPayments)
                .build();
    }

    private long toLong(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof Long) return (Long) obj;
        if (obj instanceof Integer) return ((Integer) obj).longValue();
        if (obj instanceof Number) return ((Number) obj).longValue();
        return 0;
    }
}
