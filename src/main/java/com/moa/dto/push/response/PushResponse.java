package com.moa.dto.push.response;

import com.moa.domain.Push;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PushResponse {
    
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
    
    public static PushResponse fromEntity(Push push) {
        return PushResponse.builder()
                .pushId(push.getPushId())
                .receiverId(push.getReceiverId())
                .pushCode(push.getPushCode())
                .title(push.getTitle())
                .content(push.getContent())
                .moduleId(push.getModuleId())
                .moduleType(push.getModuleType())
                .sentAt(push.getSentAt())
                .readAt(push.getReadAt())
                .isRead(push.getIsRead())
                .isDeleted(push.getIsDeleted())
                .build();
    }
}