package com.moa.dto.party.response;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 파티 목록 조회 응답 DTO
 * 페이징과 필터링을 위한 간소화된 정보
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartyListResponse {

	// === 파티 기본 정보 ===
	private Integer partyId;
	private String partyStatus;
	private Integer maxMembers;
	private Integer currentMembers;
	private Integer monthlyFee;
	private LocalDate regDate;

	// === 상품 정보 ===
	private Integer productId;
	private String productName;
	private String productImage;

	// === 방장 정보 ===
	private String partyLeaderId; // 방장 사용자 ID (역할 판별용)
	private String leaderNickname;

	// === 계산 필드 ===
	private Integer remainingSlots; // 남은 자리 (maxMembers - currentMembers)
}
