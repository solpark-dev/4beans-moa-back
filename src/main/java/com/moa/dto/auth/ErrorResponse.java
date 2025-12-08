package com.moa.dto.auth;

import com.moa.common.exception.ErrorCode;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ErrorResponse {

	private final String code;
	private final String message;

	public static ErrorResponse of(String code, String message) {
		return ErrorResponse.builder().code(code).message(message).build();
	}

	public static ErrorResponse of(ErrorCode errorCode) {
		return of(errorCode.getCode(), errorCode.getMessage());
	}
}
