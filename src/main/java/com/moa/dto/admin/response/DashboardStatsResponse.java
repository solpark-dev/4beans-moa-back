package com.moa.dto.admin.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardStatsResponse {
    private long totalRevenue;
    private long activePartyCount;
    private long totalPartyCount;
    private long totalUserCount;

    private long todayNewUsers;
    private long thisMonthRevenue;
    private long recruitingPartyCount;
    private long pendingPaymentCount;
    private long completedPaymentCount;
    private long thisMonthPaymentCount;

    private List<OttPartyStats> ottPartyStats;

    private List<DailyRevenue> dailyRevenues;

    private List<RecentUser> recentUsers;

    private List<RecentPayment> recentPayments;

    // Phase 1: 월별 매출
    private List<MonthlyRevenue> monthlyRevenues;

    // Phase 2: 증감률
    private Double revenueTrend;
    private Double userTrend;
    private Double todayUserTrend;

    // Phase 3: 주간 사용자 추이
    private List<WeeklyUserStats> weeklyNewUsers;
    private List<WeeklyUserStats> weeklyActiveUsers;

    // Phase 4: 실시간 알림
    private List<AlertItem> alerts;

    @Getter
    @Builder
    public static class OttPartyStats {
        private String ottName;
        private long partyCount;
        private long activeCount;
    }

    @Getter
    @Builder
    public static class DailyRevenue {
        private String date;
        private long amount;
    }

    @Getter
    @Builder
    public static class RecentUser {
        private String odUserId;
        private String userName;
        private String userEmail;
        private String regDate;
    }

    @Getter
    @Builder
    public static class RecentPayment {
        private Long paymentId;
        private String odUserId;
        private long amount;
        private String status;
        private String paymentDate;
        private String partyName;
    }

    // Phase 1: 월별 매출
    @Getter
    @Builder
    public static class MonthlyRevenue {
        private String month;   // "2024-12"
        private String label;   // "12월"
        private long amount;
    }

    // Phase 3: 주간 사용자 추이
    @Getter
    @Builder
    public static class WeeklyUserStats {
        private String week;    // "1주 전", "2주 전", ...
        private long count;
    }

    // Phase 4: 실시간 알림
    @Getter
    @Builder
    public static class AlertItem {
        private String type;      // "error", "warning", "info"
        private String title;
        private String message;
        private String time;      // "5분 전", "1시간 전"
    }
}
