package com.moa.dto.party.request;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class PartyCreateRequest {

	@NotNull(message = "상품 ID는 필수입니다.")
	private Integer productId;

	@NotNull(message = "최대 인원은 필수입니다.")
	@Min(value = 2, message = "최대 인원은 최소 2명입니다.")
	@Max(value = 10, message = "최대 인원은 최대 10명입니다.")
	private Integer maxMembers;

	@NotNull(message = "파티 시작일은 필수입니다.")
	private LocalDate startDate;

	private LocalDate endDate;

	@Size(max = 100, message = "OTT 계정 ID는 최대 100자까지 입력 가능합니다.")
	private String ottId;

	@Size(max = 255, message = "OTT 계정 비밀번호는 최대 255자까지 입력 가능합니다.")
	private String ottPassword;

	private Integer accountId;
}