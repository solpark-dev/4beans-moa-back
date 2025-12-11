package com.moa.dao.admin;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;

@SpringBootTest
public class AdminDashboardDaoTest {

    @Autowired
    private AdminDashboardDao adminDashboardDao;

    @Test
    void getTotalRevenue_ShouldReturnLong() {
        long revenue = adminDashboardDao.getTotalRevenue();
        assertThat(revenue).isGreaterThanOrEqualTo(0);
    }

    @Test
    void getActivePartyCount_ShouldReturnLong() {
        long count = adminDashboardDao.getActivePartyCount();
        assertThat(count).isGreaterThanOrEqualTo(0);
    }
}
