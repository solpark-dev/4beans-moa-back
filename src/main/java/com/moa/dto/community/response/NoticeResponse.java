package com.moa.dto.community.response;

import com.moa.domain.Community;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeResponse {
    
    private Integer communityId;
    private String userId;
    private Integer communityCodeId;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private Integer viewCount;
    private String category;
    private String categoryName;
    
    public static NoticeResponse fromEntity(Community community) {
        return NoticeResponse.builder()
                .communityId(community.getCommunityId())
                .userId(community.getUserId())
                .communityCodeId(community.getCommunityCodeId())
                .title(community.getTitle())
                .content(community.getContent())
                .createdAt(community.getCreatedAt())
                .viewCount(community.getViewCount())
                .category(community.getCategory())
                .categoryName(community.getCodeName())
                .build();
    }
}