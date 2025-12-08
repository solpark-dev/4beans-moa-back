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
public class InquiryResponse {
    
    private Integer communityId;
    private String userId;
    private Integer communityCodeId;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private String fileOriginal;
    private String fileUuid;
    private String answerContent;
    private LocalDateTime answeredAt;
    private String answerStatus;
    private String category;
    private String categoryName;
    
    public boolean isAnswered() {
        return "답변완료".equals(this.answerStatus);
    }
    
    public boolean hasFile() {
        return this.fileOriginal != null && !this.fileOriginal.isEmpty();
    }
    
    public static InquiryResponse fromEntity(Community community) {
        return InquiryResponse.builder()
                .communityId(community.getCommunityId())
                .userId(community.getUserId())
                .communityCodeId(community.getCommunityCodeId())
                .title(community.getTitle())
                .content(community.getContent())
                .createdAt(community.getCreatedAt())
                .fileOriginal(community.getFileOriginal())
                .fileUuid(community.getFileUuid())
                .answerContent(community.getAnswerContent())
                .answeredAt(community.getAnsweredAt())
                .answerStatus(community.getAnswerStatus())
                .category(community.getCategory())
                .categoryName(community.getCodeName())
                .build();
    }
}