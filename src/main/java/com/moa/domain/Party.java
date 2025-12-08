package com.moa.domain;

import java.time.LocalDateTime;

import com.moa.domain.enums.PartyStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 파티 도메인 클래스
 * PARTY 테이블과 1:1 매핑
 *
 * DB 스키마 기준:
 * - PARTY_ID (INT, PK, AUTO_INCREMENT)
 * - PRODUCT_ID (INT, FK → PRODUCT)
 * - PARTY_LEADER_ID (VARCHAR(50), FK → USERS)
 * - PARTY_STATUS (VARCHAR(20), DEFAULT 'PENDING_PAYMENT')
 * → PENDING_PAYMENT: 파티 생성됨, 방장 보증금 결제 대기
 * → RECRUITING: 파티원 모집 중
 * → ACTIVE: 서비스 이용 중 (최대 인원 도달)
 * → CLOSED: 파티 종료
 * - MAX_MEMBERS (INT)
 * - CURRENT_MEMBERS (INT, DEFAULT 1)
 * - MONTHLY_FEE (INT) - 월 구독료
 * - OTT_ID (VARCHAR(100))
 * - OTT_PASSWORD (VARCHAR(255))
 * - ACCOUNT_ID (INT, FK → ACCOUNT)
 * - REG_DATE (DATETIME)
 * - START_DATE (DATETIME) - 파티 시작일 (결제 기준일)
 * - END_DATE (DATETIME)
 * - LEADER_DEPOSIT_ID (INT, FK → DEPOSIT) - 방장 보증금 ID
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Party {

	private Integer partyId; // 파티 ID (PK)
	private Integer productId; // 상품 ID (FK)
	private String partyLeaderId; // 방장 사용자 ID (FK)
	private PartyStatus partyStatus; // 파티 상태 (PENDING_PAYMENT, RECRUITING, ACTIVE, CLOSED)
	private Integer maxMembers; // 최대 인원
	private Integer currentMembers; // 현재 인원 (기본값: 1, 방장 포함)
	private Integer monthlyFee; // 월 구독료
	private String ottId; // OTT 계정 ID
	private String ottPassword; // OTT 계정 비밀번호
	private Integer accountId; // 정산 계좌 ID (FK)
	private LocalDateTime regDate; // 생성일시
	private LocalDateTime startDate; // 파티 시작일시 (결제 기준일)
	private LocalDateTime endDate; // 종료일시
	private Integer leaderDepositId; // 방장 보증금 ID (FK)
}