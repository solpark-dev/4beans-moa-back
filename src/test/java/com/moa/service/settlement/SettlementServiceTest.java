package com.moa.service.settlement;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.moa.dao.account.AccountDao;
import com.moa.dao.party.PartyDao;
import com.moa.dao.payment.PaymentDao;
import com.moa.dao.settlement.SettlementDao;
import com.moa.dao.settlement.SettlementDetailDao;
import com.moa.domain.Account;
import com.moa.domain.Party;
import com.moa.domain.Settlement;
import com.moa.domain.SettlementDetail;
import com.moa.dto.payment.response.PaymentResponse;
import com.moa.service.settlement.impl.SettlementServiceImpl;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @InjectMocks
    private SettlementServiceImpl settlementService;

    @Mock
    private SettlementDao settlementDao;

    @Mock
    private SettlementDetailDao settlementDetailDao;

    @Mock
    private PaymentDao paymentDao;

    @Mock
    private PartyDao partyDao;

    @Mock
    private AccountDao accountDao;

    @Test
    @DisplayName("월별 정산 생성 시 수수료와 정산액이 정확히 계산된다")
    void createMonthlySettlement_CalculatesCorrectly() {
        // given
        Integer partyId = 1;
        String targetMonth = "2025-11";
        String leaderId = "leader";

        Party party = Party.builder().partyId(partyId).partyLeaderId(leaderId).build();
        Account account = Account.builder().accountId(100).userId(leaderId).build();

        // 결제 내역 2건 (각 10,000원)
        PaymentResponse p1 = PaymentResponse.builder()
                .paymentId(1)
                .paymentAmount(10000)
                .paymentStatus("COMPLETED")
                .targetMonth(targetMonth)
                .userId("user1")
                .partyMemberId(10)
                .build();

        PaymentResponse p2 = PaymentResponse.builder()
                .paymentId(2)
                .paymentAmount(10000)
                .paymentStatus("COMPLETED")
                .targetMonth(targetMonth)
                .userId("user2")
                .partyMemberId(11)
                .build();

        List<PaymentResponse> payments = Arrays.asList(p1, p2);

        when(settlementDao.findByPartyIdAndMonth(partyId, targetMonth)).thenReturn(Optional.empty());
        when(partyDao.findById(partyId)).thenReturn(Optional.of(party));
        when(accountDao.findByUserId(leaderId)).thenReturn(Optional.of(account));
        when(paymentDao.findByPartyId(partyId)).thenReturn(payments);

        // when
        Settlement settlement = settlementService.createMonthlySettlement(partyId, targetMonth);

        // then
        assertThat(settlement).isNotNull();
        assertThat(settlement.getTotalAmount()).isEqualTo(20000); // 10000 + 10000
        assertThat(settlement.getCommissionAmount()).isEqualTo(3000); // 20000 * 0.15
        assertThat(settlement.getNetAmount()).isEqualTo(17000); // 20000 - 3000

        verify(settlementDao).insertSettlement(any(Settlement.class));
        verify(settlementDetailDao, times(2)).insertSettlementDetail(any(SettlementDetail.class));
    }

    @Test
    @DisplayName("N+1 문제 해결 확인: 결제 상세 조회를 위한 추가 쿼리가 발생하지 않는다")
    void createMonthlySettlement_NoNPlusOneQuery() {
        // given
        Integer partyId = 1;
        String targetMonth = "2025-11";
        String leaderId = "leader";

        Party party = Party.builder().partyId(partyId).partyLeaderId(leaderId).build();
        Account account = Account.builder().accountId(100).userId(leaderId).build();

        PaymentResponse p1 = PaymentResponse.builder()
                .paymentId(1)
                .paymentAmount(10000)
                .paymentStatus("COMPLETED")
                .targetMonth(targetMonth)
                .userId("user1")
                .partyMemberId(10)
                .build();

        List<PaymentResponse> payments = Arrays.asList(p1);

        when(settlementDao.findByPartyIdAndMonth(partyId, targetMonth)).thenReturn(Optional.empty());
        when(partyDao.findById(partyId)).thenReturn(Optional.of(party));
        when(accountDao.findByUserId(leaderId)).thenReturn(Optional.of(account));
        when(paymentDao.findByPartyId(partyId)).thenReturn(payments);

        // when
        settlementService.createMonthlySettlement(partyId, targetMonth);

        // then
        // paymentDao.findById()가 호출되지 않아야 함 (N+1 해결 검증)
        verify(paymentDao, never()).findById(anyInt());
    }
}
