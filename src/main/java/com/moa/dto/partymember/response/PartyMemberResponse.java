package com.moa.dto.partymember.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 파티 멤버 응답 DTO
 * 파티 멤버 목록 조회 시 사용
 * 
 * DB 스키마 기준:
 * - PARTY_MEMBER_ID (INT)
 * - PARTY_ID (INT)
 * - USER_ID (VARCHAR(50))
 * - MEMBER_ROLE (VARCHAR(20)) - LEADER, MEMBER
 * - MEMBER_STATUS (VARCHAR(20)) - ACTIVE, LEFT
 * - JOIN_DATE (DATETIME)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartyMemberResponse {

	// === 파티 멤버 정보 ===
	private Integer partyMemberId;
	private Integer partyId;
	private String userId;
	private String memberRole;           // LEADER, MEMBER
	private String memberStatus;         // ACTIVE, LEFT
	private LocalDateTime joinDate;      // 참여일시
	
	// === 사용자 정보 (JOIN) ===
	private String nickname;
	private String profileImage;
}
