package com.moa.common.exception;

import lombok.Getter;

@Getter
public class TossPaymentException extends BusinessException {

	private final String tossErrorCode;

	public TossPaymentException(ErrorCode errorCode, String tossErrorCode, String customMessage) {
		super(errorCode, customMessage);
		this.tossErrorCode = tossErrorCode;
	}
}
