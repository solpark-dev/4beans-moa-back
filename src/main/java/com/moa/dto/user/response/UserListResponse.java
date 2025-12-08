package com.moa.dto.user.response;

import java.time.LocalDate;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserListResponse {

    @Getter
    @Builder
    public static class UserSummary {
    	private String userId;
        private String nickname;
        private String status;
        private LocalDate regDate;
        private LocalDate lastLoginDate;
    }

    private List<UserSummary> users;
    private long totalElements;
    private int page;
    private int size;
}