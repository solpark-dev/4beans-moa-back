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
public class Community {
    
    // COMMUNITY 테이블 기본 필드
    private Integer communityId;        // 커뮤니티 게시글 ID (PK)
    private String userId;              // 작성자 ID (FK -> USERS)
    private Integer communityCodeId;    // 커뮤니티 코드 ID (FK -> COMMUNITY_CODE)
    private String title;               // 게시글 제목
    private String content;             // 게시글 내용
    private LocalDateTime createdAt;    // 작성일시 
    private Integer viewCount;          // 조회수 *공지/FAQ 전용*
    private String fileOriginal;        // 첨부파일 원본명 *문의전용*
    private String fileUuid;            // 첨부파일 UUID명 *문의전용*
    private String answerContent;       // 답변 내용 *문의전용*
    private LocalDateTime answeredAt;   // 답변일시 *문의전용*
    private String answerStatus;        // 답변 상태 (답변대기/답변완료) *문의전용*
    
    // JOIN용 추가 필드
    private String nickname;            // 작성자 닉네임 (USERS 테이블)
    private String category;            // 카테고리 (COMMUNITY_CODE 테이블: INQUIRY/POST)
    private String codeName;            // 코드명 (COMMUNITY_CODE 테이블: 회원/결제/기타/FAQ 등)
}