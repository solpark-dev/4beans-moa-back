package com.moa.dto.party.response;

import java.time.LocalDateTime;

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
public class PartyMemberResponse {

	private Integer partyMemberId;
	private Integer partyId;
	private String userId;
	private String memberRole;
	private String memberStatus;
	private LocalDateTime joinDate;

	private String nickname;
	private String profileImage;
}
