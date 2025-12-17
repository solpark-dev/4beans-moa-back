package com.moa.common.event;

import lombok.Getter;

@Getter
public class UserDeletedEvent {

	private final String userId;
	private final String deleteType;
	private final String deleteReason;

	public UserDeletedEvent(String userId, String deleteType, String deleteReason) {
		this.userId = userId;
		this.deleteType = deleteType;
		this.deleteReason = deleteReason;
	}

	public static UserDeletedEvent of(String userId, String deleteType, String deleteReason) {
		return new UserDeletedEvent(userId, deleteType, deleteReason);
	}

	@Override
	public String toString() {
		return String.format("UserDeletedEvent{userId='%s', deleteType='%s', deleteReason='%s'}", userId, deleteType,
				deleteReason);
	}
}
