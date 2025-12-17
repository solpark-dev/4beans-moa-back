package com.moa.dto.payment.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class PaymentRequest {

	@NotNull(message = "결제 금액은 필수입니다.")
	private Integer amount;

	@NotBlank(message = "결제 수단은 필수입니다.")
	private String paymentMethod;
	private String tossPaymentKey;
	private String orderId;
}