package com.moa.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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