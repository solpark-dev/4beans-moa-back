package com.moa.dto.openbanking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InquiryVerifyRequest {

	@NotBlank(message = "거래고유번호는 필수입니다")
	private String bankTranId;

	@NotBlank(message = "인증코드는 필수입니다")
	@Size(min = 4, max = 4, message = "인증코드는 4자리입니다")
	private String verifyCode;
}
