package com.moa.domain;

import java.time.LocalDateTime;

import com.moa.domain.enums.PartyStatus;

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
public class Party {

	private Integer partyId;
	private Integer productId;
	private String partyLeaderId;
	private PartyStatus partyStatus;
	private Integer maxMembers;
	private Integer currentMembers;
	private Integer monthlyFee;
	private String ottId;
	private String ottPassword;
	private Integer accountId;
	private LocalDateTime regDate;
	private LocalDateTime startDate;
	private LocalDateTime endDate;
}