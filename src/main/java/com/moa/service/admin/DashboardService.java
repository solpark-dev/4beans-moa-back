package com.moa.service.admin;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moa.dao.party.PartyDao;
import com.moa.dao.payment.PaymentDao;
import com.moa.dao.user.UserDao;
import com.moa.dto.admin.DashboardStatsResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final UserDao userDao;
    private final PartyDao partyDao;
    private final PaymentDao paymentDao;

    public DashboardStatsResponse getStats() {
        long totalUserCount = userDao.countUserList(null, null, null);
        long totalPartyCount = partyDao.countAllParties();
        long activePartyCount = partyDao.countActiveParties();
        long totalRevenue = paymentDao.calculateTotalRevenue();

        return DashboardStatsResponse.builder()
                .totalUserCount(totalUserCount)
                .totalPartyCount(totalPartyCount)
                .activePartyCount(activePartyCount)
                .totalRevenue(totalRevenue)
                .build();
    }
}
