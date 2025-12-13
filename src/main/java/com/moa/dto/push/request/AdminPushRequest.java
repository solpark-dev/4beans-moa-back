package com.moa.dto.push.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 관리자 수동 푸시 발송 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminPushRequest {

    /**
     * 발송 타입
     * - TEMPLATE: 기존 템플릿 사용
     * - CUSTOM: 직접 입력
     */
    private String sendType;

    /**
     * 수신자 목록 (userId 리스트)
     */
    private List<String> receiverIds;

    /**
     * 템플릿 사용 시: 푸시 코드명
     */
    private String pushCode;

    /**
     * 템플릿 사용 시: 파라미터
     */
    private Map<String, String> params;

    /**
     * 직접 입력 시: 제목
     */
    private String title;

    /**
     * 직접 입력 시: 내용
     */
    private String content;

    /**
     * 연결 모듈 ID (선택)
     */
    private String moduleId;

    /**
     * 연결 모듈 타입 (선택)
     */
    private String moduleType;
}