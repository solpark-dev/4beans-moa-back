package com.moa.dto.party.response;

import java.time.LocalDate;
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
public class PartyDetailResponse {

	private Integer partyId;
	private String partyLeaderId;
	private String partyStatus;
	private Integer maxMembers;
	private Integer currentMembers;
	private Integer monthlyFee;
	private String ottId;
	private String ottPassword;
	private LocalDate regDate;
	private LocalDateTime startDate;
	private LocalDateTime endDate;

	private Integer productId;
	private String productName;
	private String productImage;
	private Integer price;

	private String leaderNickname;
	private String leaderProfileImage;

	private com.moa.domain.enums.MemberStatus memberStatus;
}
