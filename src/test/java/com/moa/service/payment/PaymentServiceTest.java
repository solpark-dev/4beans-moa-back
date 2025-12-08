package com.moa.service.payment;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.moa.dao.party.PartyDao;
import com.moa.dao.partymember.PartyMemberDao;
import com.moa.dao.payment.PaymentDao;
import com.moa.domain.Party;

import com.moa.domain.Payment;
import com.moa.domain.enums.MemberStatus;
import com.moa.domain.enums.PartyStatus;
import com.moa.dto.partymember.response.PartyMemberResponse;
import com.moa.dto.payment.request.PaymentRequest;
import com.moa.service.payment.impl.PaymentServiceImpl;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

        @InjectMocks
        private PaymentServiceImpl paymentService;

        @Mock
        private PaymentDao paymentDao;

        @Mock
        private TossPaymentService tossPaymentService;

        @Test
        @DisplayName("초기 결제 성공 시 Payment가 생성된다")
        void createInitialPayment_Success() {
                // given
                Integer partyId = 1;
                Integer partyMemberId = 10;
                String userId = "user1";
                Integer amount = 10000;
                String targetMonth = "2025-12";
                PaymentRequest request = PaymentRequest.builder()
                                .amount(amount)
                                .paymentMethod("CARD")
                                .tossPaymentKey("tossKey")
                                .orderId("orderId")
                                .build();

                when(paymentDao.findByPartyMemberIdAndTargetMonth(anyInt(), anyString())).thenReturn(Optional.empty());
                doNothing().when(tossPaymentService).confirmPayment(anyString(), anyString(), anyInt());

                // when
                Payment payment = paymentService.createInitialPayment(partyId, partyMemberId, userId, amount,
                                targetMonth, request);

                // then
                assertThat(payment).isNotNull();
                assertThat(payment.getPaymentAmount()).isEqualTo(amount);
                verify(tossPaymentService).confirmPayment(request.getTossPaymentKey(), request.getOrderId(), amount);
                verify(paymentDao).insertPayment(any(Payment.class));
        }
}
