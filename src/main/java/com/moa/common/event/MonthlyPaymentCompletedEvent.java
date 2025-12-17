package com.moa.common.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class MonthlyPaymentCompletedEvent {

	private final Integer partyId;

	private final Integer partyMemberId;

	private final String userId;

	private final Integer amount;

	private final String targetMonth;
}
