package com.moa.dto.admin.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MonthlyGoalResponse {
    private Long goalAmount;
    private String targetMonth; // "2025-12" 형식
}
