package com.moa.common.event;

import lombok.Getter;

/**
 * 사용자 삭제/탈퇴 이벤트
 *
 * 사용자가 탈퇴하거나 관리자에 의해 삭제될 때 발행되어
 * 파티장/파티원 처리를 트리거합니다.
 *
 * @author MOA Team
 * @since 2025-12-12
 */
@Getter
public class UserDeletedEvent {

    private final String userId;
    private final String deleteType;  // USER_REQUEST, ADMIN_DELETE, BLACKLIST 등
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
        return String.format("UserDeletedEvent{userId='%s', deleteType='%s', deleteReason='%s'}",
                userId, deleteType, deleteReason);
    }
}
