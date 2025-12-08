package com.moa.dto.user.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommonCheckResponse {
	private boolean available;
}
