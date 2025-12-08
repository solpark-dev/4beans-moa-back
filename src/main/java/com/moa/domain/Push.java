package com.moa.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Push {
    private Integer pushId;
    private String receiverId;
    private String pushCode;
    private String title;
    private String content;
    private String moduleId;
    private String moduleType;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;
    private String isRead;
    private String isDeleted;
}