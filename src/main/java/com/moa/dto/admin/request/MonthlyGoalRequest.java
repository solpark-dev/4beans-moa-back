package com.moa.dto.admin.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MonthlyGoalRequest {
    private Long goalAmount;
}
