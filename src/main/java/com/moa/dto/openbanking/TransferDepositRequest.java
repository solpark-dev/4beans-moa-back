package com.moa.dto.openbanking;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferDepositRequest {

	@NotBlank(message = "핀테크이용번호는 필수입니다")
	private String fintechUseNum;

	@NotNull(message = "이체금액은 필수입니다")
	@Min(value = 1, message = "이체금액은 1원 이상이어야 합니다")
	private Integer tranAmt;

	private String printContent;

	private String reqClientName;
}
