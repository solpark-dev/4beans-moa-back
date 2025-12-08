package com.moa.common.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class SettlementCompletedEvent {
    private final Integer partyId;
    private final Integer amount;
    private final String leaderId;
}
