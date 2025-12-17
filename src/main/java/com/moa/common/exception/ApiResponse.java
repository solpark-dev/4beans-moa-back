package com.moa.common.exception;

public class ApiResponse<T> {

	private final boolean success;
	private final T data;
	private final ApiError error;

	private ApiResponse(boolean success, T data, ApiError error) {
		this.success = success;
		this.data = data;
		this.error = error;
	}

	public static <T> ApiResponse<T> success(T data) {
		return new ApiResponse<>(true, data, null);
	}

	public static <T> ApiResponse<T> error(ErrorCode errorCode, String message) {
		return new ApiResponse<>(false, null, new ApiError(errorCode.getCode(), message));
	}

	public boolean isSuccess() {
		return success;
	}

	public T getData() {
		return data;
	}

	public ApiError getError() {
		return error;
	}
}
