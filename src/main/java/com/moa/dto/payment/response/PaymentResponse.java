package com.moa.dto.payment.response;

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
public class PaymentResponse {

	private Integer paymentId;
	private Integer partyId;
	private String userId;
	private Integer partyMemberId;
	private String paymentType;
	private Integer paymentAmount;
	private String paymentStatus;
	private String paymentMethod;
	private LocalDateTime paymentDate;
	private String targetMonth;

	private String productName;
	private String partyLeaderNickname;
	private String userNickname;

	private String cardNumber;
	private String cardCompany;

	private String retryStatus;
	private Integer attemptNumber;
	private LocalDateTime nextRetryDate;
	private String retryReason;
	private String errorMessage;
}