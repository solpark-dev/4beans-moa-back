package com.moa.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

	SUCCESS("S000", "성공", HttpStatus.OK),

	BAD_REQUEST("E400", "잘못된 요청입니다.", HttpStatus.BAD_REQUEST),
	UNAUTHORIZED("E401", "인증이 필요합니다.", HttpStatus.UNAUTHORIZED),
	FORBIDDEN("E403", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN), NOT_FOUND("E404", "대상을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
	VALIDATION_ERROR("E422", "유효성 검증 실패입니다.", HttpStatus.UNPROCESSABLE_ENTITY),
	CONFLICT("E409", "요청 충돌이 발생했습니다.", HttpStatus.CONFLICT),
	INTERNAL_ERROR("E999", "서버 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
	FEATURE_NOT_AVAILABLE("F400", "현재 버전에서는 지원하지 않는 기능입니다.", HttpStatus.BAD_REQUEST),
	EMBEDDING_FAILED("E900", "임베딩 생성 실패", HttpStatus.INTERNAL_SERVER_ERROR),

	SUBSCRIPTION_ID_REQUIRED("V002", "구독 ID는 필수입니다.", HttpStatus.BAD_REQUEST),
	SUBSCRIPTION_NOT_FOUND("SB404", "구독 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

	PARTY_NOT_FOUND("P404", "파티를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
	PARTY_FULL("P409", "파티 인원이 가득 찼습니다.", HttpStatus.CONFLICT),
	ALREADY_JOINED("P409", "이미 참여 중입니다.", HttpStatus.CONFLICT),
	LEADER_CANNOT_JOIN("P403", "파티장은 참여할 수 없습니다.", HttpStatus.FORBIDDEN),
	LEADER_CANNOT_LEAVE("P403", "파티장은 탈퇴할 수 없습니다.", HttpStatus.FORBIDDEN),
	NOT_PARTY_LEADER("P403", "파티장이 아닙니다.", HttpStatus.FORBIDDEN),
	NOT_PARTY_MEMBER("P404", "파티 참여자가 아닙니다.", HttpStatus.NOT_FOUND),
	PARTY_ALREADY_MATCHED("P409", "이미 매칭된 파티입니다.", HttpStatus.CONFLICT),
	PARTY_MEMBER_NOT_FOUND("P404", "파티 구성원을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
	INVALID_PARTY_STATUS("P400", "잘못된 파티 상태입니다.", HttpStatus.BAD_REQUEST),
	INVALID_MAX_MEMBERS("P400", "파티 최대 인원이 잘못되었습니다.", HttpStatus.BAD_REQUEST),
	PARTY_NOT_RECRUITING("P400", "모집 중인 파티가 아닙니다.", HttpStatus.BAD_REQUEST),

	BUSINESS_ERROR("E500", "이메일 발송 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),

	// 정산(Settlement) 관련 에러
	SETTLEMENT_NOT_FOUND("ST404", "정산 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
	DUPLICATE_SETTLEMENT("ST409", "이미 정산된 내역입니다.", HttpStatus.CONFLICT),
	SETTLEMENT_ALREADY_COMPLETED("ST409", "이미 완료된 정산입니다.", HttpStatus.CONFLICT),
	SETTLEMENT_FAILED("ST500", "정산 처리 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
	SETTLEMENT_PERIOD_NOT_COMPLETED("ST400", "정산 기간이 아직 완료되지 않았습니다.", HttpStatus.BAD_REQUEST),

	// 계좌(Account) 관련 에러
	ACCOUNT_NOT_FOUND("ACC404", "계좌 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
	ACCOUNT_NOT_VERIFIED("ACC400", "인증되지 않은 계좌입니다.", HttpStatus.BAD_REQUEST),

	// 보증금(Deposit) 관련 에러
	DEPOSIT_NOT_FOUND("DEP404", "보증금 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
	DEPOSIT_ALREADY_PAID("DEP409", "이미 결제된 보증금입니다.", HttpStatus.CONFLICT),
	DEPOSIT_ALREADY_REFUNDED("DEP410", "이미 환불된 보증금입니다.", HttpStatus.CONFLICT),
	DEPOSIT_AMOUNT_MISMATCH("DEP400", "보증금 금액이 일치하지 않습니다.", HttpStatus.BAD_REQUEST),

	// 결제(Payment) 관련 에러
	PAYMENT_NOT_FOUND("PAY404", "결제 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
	DUPLICATE_PAYMENT("PAY409", "이미 해당 월에 결제한 내역이 있습니다.", HttpStatus.CONFLICT),
	PAYMENT_FAILED("PAY500", "결제 처리 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
	BILLING_KEY_NOT_FOUND("PAY404", "빌링키를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
	INVALID_PAYMENT_AMOUNT("PAY400", "결제 금액이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),

	// Payment Retry Errors
	PAYMENT_RETRY_FAILED("PAY501", "결제 재시도에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
	MAX_RETRY_EXCEEDED("PAY502", "최대 재시도 횟수를 초과했습니다.", HttpStatus.BAD_REQUEST),
	RETRY_NOT_FOUND("PAY503", "재시도 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
	INVALID_PAYMENT_STATUS("PAY400", "잘못된 결제 상태입니다.", HttpStatus.BAD_REQUEST),

	// 상품(Product) 관련 에러
	PRODUCT_NOT_FOUND("PR404", "상품을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
	PRODUCT_ID_REQUIRED("V001", "상품 ID는 필수입니다.", HttpStatus.BAD_REQUEST),
	FILE_EMPTY("F400", "파일이 비어있습니다.", HttpStatus.BAD_REQUEST),

	// 유효성 검증 관련 에러
	START_DATE_REQUIRED("V004", "파티 시작일은 필수입니다.", HttpStatus.BAD_REQUEST),
	OTT_ID_REQUIRED("V006", "OTT 계정 ID는 필수입니다.", HttpStatus.BAD_REQUEST),
	OTT_PASSWORD_REQUIRED("V007", "OTT 계정 비밀번호는 필수입니다.", HttpStatus.BAD_REQUEST),
	BACKUP_CODE_ALREADY_ISSUED("E450", "이미 발급된 백업 코드가 있습니다.", HttpStatus.BAD_REQUEST),

	// 회원(User) 관련 에러
	USER_NOT_FOUND("U404", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
	DUPLICATED_PHONE("E013", "이미 사용중인 휴대폰번호입니다.", HttpStatus.CONFLICT),
	ACCOUNT_WITHDRAW("U410", "탈퇴한 계정입니다.", HttpStatus.FORBIDDEN),
	ACCOUNT_BLOCKED("U403", "블랙리스트 계정입니다. 이용이 제한되었습니다.", HttpStatus.FORBIDDEN),
	INVALID_LOGIN("U401", "아이디 또는 비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED),
	INVALID_PARAMETER("E400", "잘못된 요청 파라미터입니다.", HttpStatus.BAD_REQUEST),
	INVALID_INPUT_VALUE("E444", "잘못된 입력값입니다.", HttpStatus.BAD_REQUEST);

	private final String code;
	private final String message;
	private final HttpStatus httpStatus;

	ErrorCode(String code, String message, HttpStatus httpStatus) {
		this.code = code;
		this.message = message;
		this.httpStatus = httpStatus;
	}

	public String getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}

	public HttpStatus getHttpStatus() {
		return httpStatus;
	}
}