package com.moa.dto.openbanking;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InquiryReceiveResponse {

	private String rspCode;
	private String rspMessage;
	private String bankTranId;
	private String printContent;
	private String maskedAccount;
	private LocalDateTime expiresAt;

	public static InquiryReceiveResponse success(String bankTranId, String printContent) {
		return InquiryReceiveResponse.builder().rspCode("A0000").rspMessage("성공").bankTranId(bankTranId)
				.printContent(printContent).build();
	}

	public static InquiryReceiveResponse success(String bankTranId, String printContent, String maskedAccount,
			LocalDateTime expiresAt) {
		return InquiryReceiveResponse.builder().rspCode("A0000").rspMessage("성공").bankTranId(bankTranId)
				.printContent(printContent).maskedAccount(maskedAccount).expiresAt(expiresAt).build();
	}

	public static InquiryReceiveResponse error(String rspCode, String rspMessage) {
		return InquiryReceiveResponse.builder().rspCode(rspCode).rspMessage(rspMessage).build();
	}
}
