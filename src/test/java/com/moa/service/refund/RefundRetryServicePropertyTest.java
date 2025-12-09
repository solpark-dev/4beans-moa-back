package com.moa.service.refund;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.moa.dao.deposit.DepositDao;
import com.moa.dao.refund.RefundRetryHistoryDao;
import com.moa.domain.Deposit;
import com.moa.domain.RefundRetryHistory;
import com.moa.domain.enums.DepositStatus;
import com.moa.service.payment.TossPaymentService;
import com.moa.service.refund.impl.RefundRetryServiceImpl;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;
import net.jqwik.api.lifecycle.BeforeTry;

/**
 * Property-Based Tests for RefundRetryService
 * 
 * **Feature: transaction-compensation, Property 5: 환불 실패 이력 트랜잭션 독립성**
 * **Feature: transaction-compensation, Property 11: retryType 분기 처리**
 * **Validates: Requirements 2.1, 2.2, 5.2, 5.3, 5.4**
 */
class RefundRetryServicePropertyTest {

    private RefundRetryHistoryDao retryDao;
    private DepositDao depositDao;
    private TossPaymentService tossPaymentService;
    private RefundRetryServiceImpl refundRetryService;

    @BeforeTry
    void setUp() {
        retryDao = Mockito.mock(RefundRetryHistoryDao.class);
        depositDao = Mockito.mock(DepositDao.class);
        tossPaymentService = Mockito.mock(TossPaymentService.class);
        refundRetryService = new RefundRetryServiceImpl(retryDao, depositDao, tossPaymentService);
    }

    // ========================================
    // Property 5: 환불 실패 이력 트랜잭션 독립성
    // ========================================

    /**
     * **Feature: transaction-compensation, Property 5: 환불 실패 이력 트랜잭션 독립성**
     * **Validates: Requirements 2.1, 2.2**
     */
    @Property(tries = 100)
    @Label("Property 5: recordFailure는 모든 유효한 Deposit에 대해 이력을 저장해야 한다")
    void recordFailure_shouldPersistHistoryForAnyValidDeposit(
            @ForAll @IntRange(min = 1, max = 100000) int depositId,
            @ForAll @IntRange(min = 1000, max = 1000000) int depositAmount,
            @ForAll("validRefundReasons") String reason,
            @ForAll("validExceptions") Exception exception
    ) {
        // Given
        Deposit deposit = Deposit.builder()
                .depositId(depositId)
                .depositAmount(depositAmount)
                .depositStatus(DepositStatus.PAID)
                .tossPaymentKey("test_payment_key_" + depositId)
                .build();

        ArgumentCaptor<RefundRetryHistory> historyCaptor = 
                ArgumentCaptor.forClass(RefundRetryHistory.class);
        when(retryDao.insertRefundRetry(any(RefundRetryHistory.class))).thenReturn(1);

        // When
        refundRetryService.recordFailure(deposit, exception, reason);

        // Then
        verify(retryDao).insertRefundRetry(historyCaptor.capture());
        RefundRetryHistory savedHistory = historyCaptor.getValue();

        assertAll(
            () -> assertEquals(depositId, savedHistory.getDepositId()),
            () -> assertEquals(depositAmount, savedHistory.getRefundAmount()),
            () -> assertEquals(reason, savedHistory.getRefundReason()),
            () -> assertEquals("REFUND", savedHistory.getRetryType()),
            () -> assertEquals("FAILED", savedHistory.getRetryStatus()),
            () -> assertEquals(1, savedHistory.getAttemptNumber()),
            () -> assertNotNull(savedHistory.getNextRetryDate()),
            () -> assertNotNull(savedHistory.getAttemptDate())
        );
    }

    /**
     * **Feature: transaction-compensation, Property 5: 환불 실패 이력 트랜잭션 독립성**
     * **Validates: Requirements 2.1, 2.2**
     */
    @Property(tries = 100)
    @Label("Property 5: nextRetryDate는 항상 현재 시간 이후여야 한다")
    void recordFailure_nextRetryDateShouldBeInFuture(
            @ForAll @IntRange(min = 1, max = 100000) int depositId,
            @ForAll @IntRange(min = 1000, max = 1000000) int depositAmount
    ) {
        // Given
        Deposit deposit = Deposit.builder()
                .depositId(depositId)
                .depositAmount(depositAmount)
                .depositStatus(DepositStatus.PAID)
                .tossPaymentKey("test_key")
                .build();

        LocalDateTime beforeCall = LocalDateTime.now();
        
        ArgumentCaptor<RefundRetryHistory> historyCaptor = 
                ArgumentCaptor.forClass(RefundRetryHistory.class);
        when(retryDao.insertRefundRetry(any(RefundRetryHistory.class))).thenReturn(1);

        // When
        refundRetryService.recordFailure(deposit, new RuntimeException("Test"), "테스트 사유");

        // Then
        verify(retryDao).insertRefundRetry(historyCaptor.capture());
        RefundRetryHistory savedHistory = historyCaptor.getValue();

        assertTrue(savedHistory.getNextRetryDate().isAfter(beforeCall),
                "nextRetryDate는 현재 시간 이후여야 함");
    }

    /**
     * **Feature: transaction-compensation, Property 5: 환불 실패 이력 트랜잭션 독립성**
     * **Validates: Requirements 2.1, 2.2**
     */
    @Property(tries = 100)
    @Label("Property 5: errorMessage는 500자를 초과하지 않아야 한다")
    void recordFailure_errorMessageShouldBeTruncatedTo500Chars(
            @ForAll @IntRange(min = 1, max = 100000) int depositId,
            @ForAll @StringLength(min = 0, max = 2000) String errorMessage
    ) {
        // Given
        Deposit deposit = Deposit.builder()
                .depositId(depositId)
                .depositAmount(10000)
                .depositStatus(DepositStatus.PAID)
                .tossPaymentKey("test_key")
                .build();

        Exception exception = new RuntimeException(errorMessage);
        
        ArgumentCaptor<RefundRetryHistory> historyCaptor = 
                ArgumentCaptor.forClass(RefundRetryHistory.class);
        when(retryDao.insertRefundRetry(any(RefundRetryHistory.class))).thenReturn(1);

        // When
        refundRetryService.recordFailure(deposit, exception, "테스트");

        // Then
        verify(retryDao).insertRefundRetry(historyCaptor.capture());
        RefundRetryHistory savedHistory = historyCaptor.getValue();

        if (savedHistory.getErrorMessage() != null) {
            assertTrue(savedHistory.getErrorMessage().length() <= 500,
                    "errorMessage는 500자를 초과하면 안 됨");
        }
    }


    // ========================================
    // Property 11: retryType 분기 처리 테스트
    // ========================================

    /**
     * **Feature: transaction-compensation, Property 11: retryType 분기 처리**
     * **Validates: Requirements 5.2, 5.3, 5.4**
     */
    @Property(tries = 100)
    @Label("Property 11: COMPENSATION 타입은 Toss 취소 후 Deposit 삭제")
    void retryRefund_compensationType_shouldCancelTossAndDeleteDeposit(
            @ForAll @IntRange(min = 1, max = 100000) int depositId,
            @ForAll @IntRange(min = 1000, max = 1000000) int refundAmount
    ) {
        // Given
        RefundRetryHistory retry = RefundRetryHistory.builder()
                .retryId(1)
                .depositId(depositId)
                .attemptNumber(1)
                .retryStatus("PENDING")
                .retryType("COMPENSATION")
                .refundAmount(refundAmount)
                .refundReason("보상 트랜잭션 - DB 실패")
                .build();

        Deposit deposit = Deposit.builder()
                .depositId(depositId)
                .depositAmount(refundAmount)
                .depositStatus(DepositStatus.PENDING)
                .tossPaymentKey("test_payment_key_" + depositId)
                .build();

        when(depositDao.findById(depositId)).thenReturn(java.util.Optional.of(deposit));
        doNothing().when(tossPaymentService).cancelPayment(anyString(), anyString(), anyInt());
        when(depositDao.deleteById(depositId)).thenReturn(1);
        when(retryDao.updateRetryStatus(any(RefundRetryHistory.class))).thenReturn(1);

        // When
        refundRetryService.retryRefund(retry);

        // Then
        verify(tossPaymentService).cancelPayment(
                eq(deposit.getTossPaymentKey()),
                anyString(),
                eq(refundAmount));
        verify(depositDao).deleteById(depositId);
        
        ArgumentCaptor<RefundRetryHistory> captor = ArgumentCaptor.forClass(RefundRetryHistory.class);
        verify(retryDao).updateRetryStatus(captor.capture());
        assertEquals("SUCCESS", captor.getValue().getRetryStatus());
    }

    /**
     * **Feature: transaction-compensation, Property 11: retryType 분기 처리**
     * **Validates: Requirements 5.2, 5.3, 5.4**
     */
    @Property(tries = 100)
    @Label("Property 11: REFUND 타입은 Deposit 상태를 REFUNDED로 변경")
    void retryRefund_refundType_shouldUpdateDepositStatusToRefunded(
            @ForAll @IntRange(min = 1, max = 100000) int depositId,
            @ForAll @IntRange(min = 1000, max = 1000000) int refundAmount
    ) {
        // Given
        RefundRetryHistory retry = RefundRetryHistory.builder()
                .retryId(1)
                .depositId(depositId)
                .attemptNumber(1)
                .retryStatus("FAILED")
                .retryType("REFUND")
                .refundAmount(refundAmount)
                .refundReason("환불 재시도")
                .build();

        Deposit deposit = Deposit.builder()
                .depositId(depositId)
                .depositAmount(refundAmount)
                .depositStatus(DepositStatus.PAID)
                .tossPaymentKey("test_payment_key_" + depositId)
                .build();

        when(depositDao.findById(depositId)).thenReturn(java.util.Optional.of(deposit));
        doNothing().when(tossPaymentService).cancelPayment(anyString(), anyString(), anyInt());
        when(depositDao.updateDeposit(any(Deposit.class))).thenReturn(1);
        when(retryDao.updateRetryStatus(any(RefundRetryHistory.class))).thenReturn(1);

        // When
        refundRetryService.retryRefund(retry);

        // Then
        ArgumentCaptor<Deposit> depositCaptor = ArgumentCaptor.forClass(Deposit.class);
        verify(depositDao).updateDeposit(depositCaptor.capture());
        assertEquals(DepositStatus.REFUNDED, depositCaptor.getValue().getDepositStatus());
        
        verify(depositDao, never()).deleteById(anyInt());
    }

    /**
     * **Feature: transaction-compensation, Property 11: retryType 분기 처리**
     * **Validates: Requirements 5.2, 5.3, 5.4**
     */
    @Property(tries = 50)
    @Label("Property 11: COMPENSATION - Deposit 이미 삭제된 경우 성공 처리")
    void retryRefund_compensationType_alreadyDeleted_shouldMarkSuccess(
            @ForAll @IntRange(min = 1, max = 100000) int depositId
    ) {
        // Given
        RefundRetryHistory retry = RefundRetryHistory.builder()
                .retryId(1)
                .depositId(depositId)
                .attemptNumber(1)
                .retryStatus("PENDING")
                .retryType("COMPENSATION")
                .refundAmount(10000)
                .build();

        when(depositDao.findById(depositId)).thenReturn(java.util.Optional.empty());
        when(retryDao.updateRetryStatus(any(RefundRetryHistory.class))).thenReturn(1);

        // When
        refundRetryService.retryRefund(retry);

        // Then
        verify(tossPaymentService, never()).cancelPayment(anyString(), anyString(), anyInt());
        
        ArgumentCaptor<RefundRetryHistory> captor = ArgumentCaptor.forClass(RefundRetryHistory.class);
        verify(retryDao).updateRetryStatus(captor.capture());
        assertEquals("SUCCESS", captor.getValue().getRetryStatus());
    }

    /**
     * **Feature: transaction-compensation, Property 11: retryType 분기 처리**
     * **Validates: Requirements 5.2, 5.3, 5.4**
     */
    @Property(tries = 50)
    @Label("Property 11: null retryType은 REFUND로 처리")
    void retryRefund_nullRetryType_shouldTreatAsRefund(
            @ForAll @IntRange(min = 1, max = 100000) int depositId,
            @ForAll @IntRange(min = 1000, max = 1000000) int refundAmount
    ) {
        // Given
        RefundRetryHistory retry = RefundRetryHistory.builder()
                .retryId(1)
                .depositId(depositId)
                .attemptNumber(1)
                .retryStatus("FAILED")
                .retryType(null)
                .refundAmount(refundAmount)
                .build();

        Deposit deposit = Deposit.builder()
                .depositId(depositId)
                .depositAmount(refundAmount)
                .depositStatus(DepositStatus.PAID)
                .tossPaymentKey("test_key")
                .build();

        when(depositDao.findById(depositId)).thenReturn(java.util.Optional.of(deposit));
        doNothing().when(tossPaymentService).cancelPayment(anyString(), anyString(), anyInt());
        when(depositDao.updateDeposit(any(Deposit.class))).thenReturn(1);
        when(retryDao.updateRetryStatus(any(RefundRetryHistory.class))).thenReturn(1);

        // When
        refundRetryService.retryRefund(retry);

        // Then
        verify(depositDao).updateDeposit(any(Deposit.class));
        verify(depositDao, never()).deleteById(anyInt());
    }

    // ========================================
    // Providers
    // ========================================

    @Provide
    Arbitrary<String> validRefundReasons() {
        return Arbitraries.of(
            "사용자 요청",
            "파티 탈퇴",
            "파티 해산",
            "결제 오류 보상",
            "시스템 오류",
            "관리자 처리"
        );
    }

    @Provide
    Arbitrary<Exception> validExceptions() {
        return Arbitraries.of(
            new RuntimeException("Toss API 호출 실패"),
            new IllegalStateException("잘못된 상태"),
            new RuntimeException("DB 연결 오류"),
            new RuntimeException("타임아웃"),
            new RuntimeException("네트워크 오류")
        );
    }
}
