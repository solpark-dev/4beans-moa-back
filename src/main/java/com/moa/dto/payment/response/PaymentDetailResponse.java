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
public class PaymentDetailResponse {

	private Integer paymentId;
	private Integer partyId;
	private Integer partyMemberId;
	private String userId;
	private String paymentType;
	private Integer paymentAmount;
	private String paymentStatus;
	private String paymentMethod;
	private LocalDateTime paymentDate;
	private String targetMonth;

	private String tossPaymentKey;
	private String orderId;
	private String cardNumber;
	private String cardCompany;

	private String productName;
	private String productImage;
	private String partyLeaderNickname;
	private String userNickname;
}