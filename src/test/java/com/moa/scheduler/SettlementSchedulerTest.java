package com.moa.scheduler;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.moa.dao.party.PartyDao;
import com.moa.domain.Party;
import com.moa.domain.Settlement;
import com.moa.service.settlement.SettlementService;

@ExtendWith(MockitoExtension.class)
class SettlementSchedulerTest {

    @InjectMocks
    private SettlementScheduler settlementScheduler;

    @Mock
    private PartyDao partyDao;

    @Mock
    private SettlementService settlementService;

    @Test
    @DisplayName("활성 파티가 있으면 정산 생성 및 완료가 호출되어야 한다")
    void runMonthlySettlement_WithActiveParties_CallsService() {
        // given
        Party party = Party.builder().partyId(1).build();
        Settlement settlement = Settlement.builder().settlementId(100).build();

        when(partyDao.findActiveParties()).thenReturn(List.of(party));
        when(settlementService.createMonthlySettlement(anyInt(), anyString())).thenReturn(settlement);
        doNothing().when(settlementService).completeSettlement(anyInt());

        // when
        settlementScheduler.runMonthlySettlement();

        // then
        verify(partyDao).findActiveParties();
        verify(settlementService).createMonthlySettlement(eq(1), anyString());
        verify(settlementService).completeSettlement(100);
    }

    @Test
    @DisplayName("활성 파티가 없으면 정산 로직이 실행되지 않아야 한다")
    void runMonthlySettlement_NoActiveParties_DoNothing() {
        // given
        when(partyDao.findActiveParties()).thenReturn(Collections.emptyList());

        // when
        settlementScheduler.runMonthlySettlement();

        // then
        verify(partyDao).findActiveParties();
        verify(settlementService, never()).createMonthlySettlement(anyInt(), anyString());
    }
}
