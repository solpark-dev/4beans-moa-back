package com.moa.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SettlementStatus {
    PENDING("정산대기"),
    PENDING_ACCOUNT("계좌대기"),
    IN_PROGRESS("처리중"),
    COMPLETED("정산완료"),
    FAILED("정산실패");

    private final String description;
}
