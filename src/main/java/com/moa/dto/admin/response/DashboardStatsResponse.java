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
}
