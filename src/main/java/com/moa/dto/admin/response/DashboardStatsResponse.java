package com.moa.dto.admin.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardStatsResponse {
    // 기본 통계
    private long totalRevenue; // 총 매출 (모든 정산 완료된 금액의 수수료 합계 or 전체 결제 금액)
    private long activePartyCount; // 현재 활성화된 파티 수
    private long totalPartyCount; // 누적 파티 수
    private long totalUserCount; // 총 회원 수

    // 추가 통계
    private long todayNewUsers; // 오늘 가입한 회원 수
    private long thisMonthRevenue; // 이번 달 매출
    private long recruitingPartyCount; // 모집 중인 파티 수
    private long pendingPaymentCount; // 결제 대기 중인 건수
    private long completedPaymentCount; // 완료된 결제 건수
    private long thisMonthPaymentCount; // 이번 달 결제 건수

    // OTT 서비스별 파티 수
    private List<OttPartyStats> ottPartyStats;

    // 최근 7일 매출 데이터
    private List<DailyRevenue> dailyRevenues;

    // 최근 가입 회원
    private List<RecentUser> recentUsers;

    // 최근 결제 내역
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
