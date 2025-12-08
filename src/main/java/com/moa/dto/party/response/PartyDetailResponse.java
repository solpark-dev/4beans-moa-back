package com.moa.dto.party.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 파티 상세 조회 응답 DTO
 * 파티 정보 + 상품 정보 + 방장 정보 포함
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartyDetailResponse {

	// === 파티 기본 정보 ===
	private Integer partyId;
	private String partyLeaderId;
	private String partyStatus; // Enum을 String으로 변환
	private Integer maxMembers;
	private Integer currentMembers;
	private Integer monthlyFee;
	private String ottId; // OTT 계정 ID
	private String ottPassword; // OTT 비밀번호
	private LocalDate regDate; // 생성일
	private LocalDateTime startDate; // 시작일시

	// === 상품 정보 (JOIN) ===
	private Integer productId;
	private String productName;
	private String productImage;
	private Integer price;

	// === 방장 정보 (JOIN) ===
	private String leaderNickname;
	private String leaderProfileImage;
}
