package com.moa.domain;

import java.time.LocalDateTime;

import com.moa.domain.enums.MemberStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 파티 멤버 도메인 클래스
 * PARTY_MEMBER 테이블과 1:1 매핑
 *
 * DB 스키마 기준:
 * - PARTY_MEMBER_ID (INT, PK, AUTO_INCREMENT)
 * - PARTY_ID (INT, FK → PARTY)
 * - USER_ID (VARCHAR(50), FK → USERS)
 * - MEMBER_ROLE (VARCHAR(20), DEFAULT 'MEMBER')
 * → LEADER: 파티장
 * → MEMBER: 일반 파티원
 * - MEMBER_STATUS (VARCHAR(20), DEFAULT 'PENDING_PAYMENT')
 * → PENDING_PAYMENT: 결제 대기 중
 * → ACTIVE: 활성 상태 (결제 완료)
 * - JOIN_DATE (DATETIME)
 * - WITHDRAW_DATE (DATETIME) - 탈퇴일시
 *
 * 비즈니스 로직:
 * - 보증금/결제 정보는 DEPOSIT, PAYMENT 테이블에서 userId+partyId로 조회
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartyMember {

	private Integer partyMemberId; // 파티 멤버 ID (PK)
	private Integer partyId; // 파티 ID (FK)
	private String userId; // 사용자 ID (FK)
	private String memberRole; // 멤버 역할 (LEADER, MEMBER)
	private MemberStatus memberStatus; // 멤버 상태 (PENDING_PAYMENT, ACTIVE)
	private LocalDateTime joinDate; // 참여일시
	private LocalDateTime withdrawDate; // 탈퇴일시
}