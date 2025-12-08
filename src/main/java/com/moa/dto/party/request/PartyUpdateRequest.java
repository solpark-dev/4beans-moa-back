package com.moa.dto.party.request;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 파티 수정 요청 DTO
 * 모집 중일 때만 수정 가능
 * 파티명과 모집 종료일만 수정 가능
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartyUpdateRequest {

	private String partyName;            // 파티명 (선택)
	private LocalDate recruitEndDate;    // 모집 종료일 (선택)
}
