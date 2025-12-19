package com.moa.service.admin.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moa.dao.admin.AdminDashboardDao;
import com.moa.dto.admin.response.DashboardStatsResponse;
import com.moa.dto.admin.response.DashboardStatsResponse.AlertItem;
import com.moa.dto.admin.response.DashboardStatsResponse.DailyRevenue;
import com.moa.dto.admin.response.DashboardStatsResponse.MonthlyRevenue;
import com.moa.dto.admin.response.DashboardStatsResponse.OttPartyStats;
import com.moa.dto.admin.response.DashboardStatsResponse.RecentPayment;
import com.moa.dto.admin.response.DashboardStatsResponse.RecentUser;
import com.moa.dto.admin.response.DashboardStatsResponse.WeeklyUserStats;
import com.moa.dto.admin.response.MonthlyGoalResponse;
import com.moa.service.admin.AdminDashboardService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

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

        // Phase 1: 월별 매출
        List<MonthlyRevenue> monthlyRevenues = adminDashboardDao.getMonthlyRevenues().stream()
                .map(m -> MonthlyRevenue.builder()
                        .month((String) m.get("month"))
                        .label((String) m.get("label"))
                        .amount(toLong(m.get("amount")))
                        .build())
                .collect(Collectors.toList());

        // Phase 2: 증감률 계산
        long lastMonthRevenue = adminDashboardDao.getLastMonthRevenue();
        long lastMonthUserCount = adminDashboardDao.getLastMonthUserCount();
        long yesterdayNewUsers = adminDashboardDao.getYesterdayNewUsers();

        Double revenueTrend = calculateTrend(thisMonthRevenue, lastMonthRevenue);
        Double userTrend = calculateTrend(totalUserCount, lastMonthUserCount);
        Double todayUserTrend = calculateTrend(todayNewUsers, yesterdayNewUsers);

        // Phase 3: 주간 사용자 추이
        List<WeeklyUserStats> weeklyNewUsers = adminDashboardDao.getWeeklyNewUsers().stream()
                .map(m -> WeeklyUserStats.builder()
                        .week((String) m.get("week"))
                        .count(toLong(m.get("count")))
                        .build())
                .collect(Collectors.toList());
        Collections.reverse(weeklyNewUsers); // 오래된 순서로 정렬

        List<WeeklyUserStats> weeklyActiveUsers = adminDashboardDao.getWeeklyActiveUsers().stream()
                .map(m -> WeeklyUserStats.builder()
                        .week((String) m.get("week"))
                        .count(toLong(m.get("count")))
                        .build())
                .collect(Collectors.toList());
        Collections.reverse(weeklyActiveUsers); // 오래된 순서로 정렬

        // Phase 4: 실시간 알림
        List<AlertItem> alerts = generateAlerts(todayNewUsers);

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
                .monthlyRevenues(monthlyRevenues)
                .revenueTrend(revenueTrend)
                .userTrend(userTrend)
                .todayUserTrend(todayUserTrend)
                .weeklyNewUsers(weeklyNewUsers)
                .weeklyActiveUsers(weeklyActiveUsers)
                .alerts(alerts)
                .build();
    }

    private long toLong(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof Long) return (Long) obj;
        if (obj instanceof Integer) return ((Integer) obj).longValue();
        if (obj instanceof Number) return ((Number) obj).longValue();
        return 0;
    }

    // Phase 2: 증감률 계산 헬퍼
    private Double calculateTrend(long current, long previous) {
        if (previous == 0) return current > 0 ? 100.0 : 0.0;
        return Math.round(((double)(current - previous) / previous) * 1000) / 10.0;
    }

    // Phase 4: 실시간 알림 생성
    private List<AlertItem> generateAlerts(long todayNewUsers) {
        List<AlertItem> alerts = new ArrayList<>();

        // 1. 결제 실패 알림
        List<Map<String, Object>> failedPayments = adminDashboardDao.getRecentFailedPayments();
        for (Map<String, Object> fp : failedPayments) {
            alerts.add(AlertItem.builder()
                .type("error")
                .title("결제 실패")
                .message(fp.get("userId") + " 님의 " + formatAmount(fp.get("amount")) + " 결제 실패")
                .time(formatTimeAgo(toLong(fp.get("minutesAgo"))))
                .build());
        }

        // 2. 만료 예정 알림
        List<Map<String, Object>> expiringParties = adminDashboardDao.getExpiringParties();
        for (Map<String, Object> ep : expiringParties) {
            alerts.add(AlertItem.builder()
                .type("warning")
                .title("만료 예정")
                .message(ep.get("ottName") + " 파티 " + ep.get("count") + "개 이번 주 만료")
                .time("이번 주")
                .build());
        }

        // 3. 신규 가입 알림
        if (todayNewUsers > 0) {
            alerts.add(AlertItem.builder()
                .type("info")
                .title("신규 가입")
                .message("오늘 신규 가입자 " + todayNewUsers + "명")
                .time("오늘")
                .build());
        }

        return alerts.stream().limit(5).collect(Collectors.toList());
    }

    private String formatTimeAgo(long minutes) {
        if (minutes < 60) return minutes + "분 전";
        if (minutes < 1440) return (minutes / 60) + "시간 전";
        return (minutes / 1440) + "일 전";
    }

    private String formatAmount(Object amount) {
        return String.format("%,d원", toLong(amount));
    }

    @Override
    @Transactional(readOnly = true)
    public MonthlyGoalResponse getMonthlyGoal() {
        String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        Long goalAmount = adminDashboardDao.getMonthlyGoal(currentMonth);

        return MonthlyGoalResponse.builder()
                .goalAmount(goalAmount != null ? goalAmount : 10000000L) // 기본값 1000만원
                .targetMonth(currentMonth)
                .build();
    }

    @Override
    @Transactional
    public MonthlyGoalResponse updateMonthlyGoal(Long goalAmount) {
        String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        // 기존 목표가 있으면 업데이트, 없으면 삽입
        Long existingGoal = adminDashboardDao.getMonthlyGoal(currentMonth);
        if (existingGoal != null) {
            adminDashboardDao.updateMonthlyGoal(currentMonth, goalAmount);
        } else {
            adminDashboardDao.insertMonthlyGoal(currentMonth, goalAmount);
        }

        return MonthlyGoalResponse.builder()
                .goalAmount(goalAmount)
                .targetMonth(currentMonth)
                .build();
    }
}
