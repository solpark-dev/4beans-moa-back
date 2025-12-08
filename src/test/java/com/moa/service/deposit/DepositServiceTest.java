package com.moa.service.deposit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.moa.common.exception.BusinessException;
import com.moa.common.exception.ErrorCode;
import com.moa.dao.deposit.DepositDao;
import com.moa.domain.Deposit;
import com.moa.domain.enums.DepositStatus;
import com.moa.service.deposit.impl.DepositServiceImpl;
import com.moa.service.payment.TossPaymentService;

@ExtendWith(MockitoExtension.class)
class DepositServiceTest {

        @InjectMocks
        private DepositServiceImpl depositService;

        @Mock
        private DepositDao depositDao;

        @Mock
        private TossPaymentService tossPaymentService;

        @Mock
        private org.springframework.context.ApplicationEventPublisher eventPublisher;

        @Test
        @DisplayName("보증금 환불 성공 테스트")
        void refundDeposit_Success() {
                // given
                Integer depositId = 1;
                Deposit deposit = Deposit.builder()
                                .depositId(depositId)
                                .depositStatus(DepositStatus.PAID)
                                .depositAmount(1000)
                                .tossPaymentKey("test_key")
                                .build();

                when(depositDao.findById(depositId)).thenReturn(Optional.of(deposit));
                doNothing().when(tossPaymentService).cancelPayment(anyString(), anyString(), anyInt());
                when(depositDao.updateDeposit(any(Deposit.class))).thenReturn(1);

                // when
                depositService.refundDeposit(depositId, "User Request");

                // then
                assertEquals(DepositStatus.REFUNDED, deposit.getDepositStatus());
                assertNotNull(deposit.getRefundDate());
                assertEquals(1000, deposit.getRefundAmount());
                verify(tossPaymentService).cancelPayment("test_key", "User Request", 1000);
                verify(depositDao).updateDeposit(deposit);
        }

        @Test
        @DisplayName("이미 환불된 보증금은 환불 실패해야 한다")
        void refundDeposit_AlreadyRefunded_ThrowsException() {
                // given
                Integer depositId = 1;
                Deposit deposit = Deposit.builder()
                                .depositId(depositId)
                                .depositStatus(DepositStatus.REFUNDED)
                                .build();

                when(depositDao.findById(depositId)).thenReturn(Optional.of(deposit));

                // when & then
                BusinessException exception = assertThrows(BusinessException.class,
                                () -> depositService.refundDeposit(depositId, "User Request"));
                assertEquals(ErrorCode.DEPOSIT_ALREADY_REFUNDED, exception.getErrorCode());
        }
}
