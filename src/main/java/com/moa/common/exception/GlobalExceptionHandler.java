package com.moa.common.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
		ErrorCode code = e.getErrorCode();
		return ResponseEntity.status(code.getHttpStatus()).body(ApiResponse.error(code, e.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
		String message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
		ErrorCode code = ErrorCode.VALIDATION_ERROR;
		return ResponseEntity.status(code.getHttpStatus()).body(ApiResponse.error(code, message));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiResponse<Void>> handleJsonParse(HttpMessageNotReadableException e) {
		ErrorCode code = ErrorCode.BAD_REQUEST;
		return ResponseEntity.status(code.getHttpStatus()).body(ApiResponse.error(code, "요청 JSON 형식이 올바르지 않습니다."));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
		System.err.println("[SYSTEM ERROR] " + e.getMessage());
		ErrorCode code = ErrorCode.INTERNAL_ERROR;
		return ResponseEntity.status(code.getHttpStatus()).body(ApiResponse.error(code, code.getMessage()));
	}
}
