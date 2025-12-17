package com.moa.dto.community.request;

import com.moa.domain.Community;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeRequest {
    
    private String userId;
    private Integer communityCodeId;
    private String title;
    private String content;
    
    public Community toEntity() {
        return Community.builder()
                .userId(this.userId)
                .communityCodeId(this.communityCodeId)
                .title(this.title)
                .content(this.content)
                .build();
    }
}