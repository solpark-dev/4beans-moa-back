package com.moa.common.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class RefundCompletedEvent {
    private final Integer depositId;
    private final Integer amount;
    private final String userId;
}
