package com.moa.domain.openbanking;

/**
 * 입금이체 거래 상태
 */
public enum TransactionStatus {
    PENDING, // 처리 대기 중
    SUCCESS, // 이체 성공
    FAILED // 이체 실패
}
