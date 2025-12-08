package com.moa.dto.party.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * OTT 계정 정보 수정 요청 DTO
 * 파티장이 파티의 OTT 계정 정보를 수정할 때 사용
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOttAccountRequest {

	/**
	 * OTT 서비스 ID (계정)
	 */
	@NotBlank(message = "OTT ID는 필수입니다.")
	@Size(max = 100, message = "OTT ID는 최대 100자까지 입력 가능합니다.")
	private String ottId;

	/**
	 * OTT 서비스 비밀번호
	 */
	@NotBlank(message = "OTT 비밀번호는 필수입니다.")
	@Size(max = 255, message = "OTT 비밀번호는 최대 255자까지 입력 가능합니다.")
	private String ottPassword;
}
