package com.moa.dto.openbanking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InquiryVerifyResponse {

	private String rspCode;
	private String rspMessage;
	private boolean verified;
	private String fintechUseNum;

	public static InquiryVerifyResponse success(String fintechUseNum) {
		return InquiryVerifyResponse.builder().rspCode("A0000").rspMessage("인증 성공").verified(true)
				.fintechUseNum(fintechUseNum).build();
	}

	public static InquiryVerifyResponse fail(String rspCode, String rspMessage) {
		return InquiryVerifyResponse.builder().rspCode(rspCode).rspMessage(rspMessage).verified(false).build();
	}
}
