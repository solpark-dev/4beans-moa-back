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
public class Community {
    
    private Integer communityId;
    private String userId;
    private Integer communityCodeId;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private Integer viewCount;
    private String fileOriginal;
    private String fileUuid;
    private String answerContent;
    private LocalDateTime answeredAt;
    private String answerStatus;

    private String nickname;
    private String category;
    private String codeName;
}