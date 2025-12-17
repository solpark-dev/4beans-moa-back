package com.moa.domain;

import java.time.LocalDateTime;

import com.moa.domain.enums.MemberStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartyMember {

	private Integer partyMemberId;
	private Integer partyId;
	private String userId;
	private String memberRole;
	private MemberStatus memberStatus;
	private LocalDateTime joinDate;
	private LocalDateTime withdrawDate;
}