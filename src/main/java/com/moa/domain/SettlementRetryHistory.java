package com.moa.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 정산 재시도 이력 도메인
 * SETTLEMENT_RETRY_HISTORY 테이블과 1:1 매핑
 *
 * 오픈뱅킹 입금이체 실패 시 재시도 이력을 추적
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementRetryHistory {

    private Integer retryId; // 재시도 이력 ID (PK)
    private Integer settlementId; // 정산 ID (FK)
    // partyId, partyLeaderId, accountId는 스키마에서 정규화로 제거됨
    // 필요 시 SETTLEMENT 테이블 JOIN으로 조회

    private Integer attemptNumber; // 시도 횟수 (1=초기, 2~4=재시도)
    private LocalDateTime attemptDate; // 시도 일시
    private String retryReason; // 재시도 사유 (이전 시도 에러 메시지)
    private String retryStatus; // 결과 상태 (PENDING/IN_PROGRESS/SUCCESS/FAILED)

    private LocalDateTime nextRetryDate; // 다음 재시도 예정 일시
    private Integer transferAmount; // 이체 금액

    // 오픈뱅킹 에러 정보
    private String errorCode; // 오픈뱅킹 에러 코드
    private String errorMessage; // 오픈뱅킹 에러 메시지
    private String bankRspCode; // 은행 응답 코드
    private String bankRspMessage; // 은행 응답 메시지

    // 성공 시 거래 정보
    private String bankTranId; // 오픈뱅킹 거래 고유번호

    private LocalDateTime createdAt; // 생성일시
    private LocalDateTime updatedAt; // 수정일시

    // 상태 상수
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
}
