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
    //private String fileOriginal;
    //private String fileUuid;
    
    public Community toEntity() {
        return Community.builder()
                .userId(this.userId)
                .communityCodeId(this.communityCodeId)
                .title(this.title)
                .content(this.content)
                //.fileOriginal(this.fileOriginal)
                //.fileUuid(this.fileUuid)
                .build();
    }
}