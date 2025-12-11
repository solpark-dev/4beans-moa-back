package com.moa.dto.admin;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardStatsResponse {
    private long totalRevenue;
    private long activePartyCount;
    private long totalPartyCount;
    private long totalUserCount;
}
