package com.moa.common.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class MonthlyPaymentFailedEvent {

	private final Integer partyId;

	private final Integer partyMemberId;

	private final String userId;

	private final String targetMonth;

	private final String errorMessage;
}
