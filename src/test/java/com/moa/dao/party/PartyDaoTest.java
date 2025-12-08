package com.moa.dao.party;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.moa.domain.Party;
import com.moa.domain.enums.PartyStatus;

/**
 * PartyDao Integration Tests
 * Tests edge cases for findPartiesByPaymentDay() method
 * Critical for handling 29/30/31 day month edge cases
 *
 * @author MOA Team
 * @since 2025-12-04
 */
@SpringBootTest
@Transactional
class PartyDaoTest {

    @Autowired
    private PartyDao partyDao;

    @Autowired
    private com.moa.dao.user.UserDao userDao;

    @Test
    @DisplayName("31일에 시작한 파티는 2월 28일에 조회된다")
    void findPartiesByPaymentDay_Feb28_FindsDay31Party() {
        // given
        String leaderId = "testLeader" + java.util.UUID.randomUUID().toString();
        createTestUser(leaderId);
        Party party = createTestParty(31, "2024-01-31", leaderId);
        partyDao.insertParty(party);

        // when - Feb 28 (non-leap year, last day = 28)
        List<Party> parties = partyDao.findPartiesByPaymentDay(28, 28);

        // then
        assertThat(parties).isNotEmpty();
        assertThat(parties).anyMatch(p -> p.getPartyId().equals(party.getPartyId()));
    }

    @Test
    @DisplayName("31일에 시작한 파티는 2월 29일에 조회된다 (윤년)")
    void findPartiesByPaymentDay_Feb29_FindsDay31Party() {
        // given
        String leaderId = "testLeader" + java.util.UUID.randomUUID().toString();
        createTestUser(leaderId);
        Party party = createTestParty(31, "2024-01-31", leaderId);
        partyDao.insertParty(party);

        // when - Feb 29 (leap year, last day = 29)
        List<Party> parties = partyDao.findPartiesByPaymentDay(29, 29);

        // then
        assertThat(parties).isNotEmpty();
        assertThat(parties).anyMatch(p -> p.getPartyId().equals(party.getPartyId()));
    }

    @Test
    @DisplayName("30일에 시작한 파티는 2월 28일에 조회된다")
    void findPartiesByPaymentDay_Feb28_FindsDay30Party() {
        // given
        String leaderId = "testLeader" + java.util.UUID.randomUUID().toString();
        createTestUser(leaderId);
        Party party = createTestParty(30, "2024-01-30", leaderId);
        partyDao.insertParty(party);

        // when - Feb 28 (non-leap year)
        List<Party> parties = partyDao.findPartiesByPaymentDay(28, 28);

        // then
        assertThat(parties).isNotEmpty();
        assertThat(parties).anyMatch(p -> p.getPartyId().equals(party.getPartyId()));
    }

    @Test
    @DisplayName("31일에 시작한 파티는 4월 30일에 조회된다")
    void findPartiesByPaymentDay_April30_FindsDay31Party() {
        // given
        String leaderId = "testLeader" + java.util.UUID.randomUUID().toString();
        createTestUser(leaderId);
        Party party = createTestParty(31, "2024-03-31", leaderId);
        partyDao.insertParty(party);

        // when - April 30 (last day = 30)
        List<Party> parties = partyDao.findPartiesByPaymentDay(30, 30);

        // then
        assertThat(parties).isNotEmpty();
        assertThat(parties).anyMatch(p -> p.getPartyId().equals(party.getPartyId()));
    }

    @Test
    @DisplayName("15일에 시작한 파티는 15일에만 조회된다")
    void findPartiesByPaymentDay_Day15_OnlyOnDay15() {
        // given
        String leaderId = "testLeader" + java.util.UUID.randomUUID().toString();
        createTestUser(leaderId);
        Party party = createTestParty(15, "2024-01-15", leaderId);
        partyDao.insertParty(party);

        // when - Query on day 15
        List<Party> partiesOn15 = partyDao.findPartiesByPaymentDay(15, 31);

        // then
        assertThat(partiesOn15).isNotEmpty();
        assertThat(partiesOn15).anyMatch(p -> p.getPartyId().equals(party.getPartyId()));

        // when - Query on day 14
        List<Party> partiesOn14 = partyDao.findPartiesByPaymentDay(14, 31);

        // then
        assertThat(partiesOn14).noneMatch(p -> p.getPartyId().equals(party.getPartyId()));
    }

    @Test
    @DisplayName("ACTIVE 상태가 아닌 파티는 조회되지 않는다")
    void findPartiesByPaymentDay_OnlyActiveParties() {
        // given
        String leaderId1 = "testLeader1_" + java.util.UUID.randomUUID().toString();
        createTestUser(leaderId1);
        Party activeParty = createTestParty(15, "2024-01-15", leaderId1);
        activeParty.setPartyStatus(PartyStatus.ACTIVE);
        partyDao.insertParty(activeParty);

        String leaderId2 = "testLeader2_" + java.util.UUID.randomUUID().toString();
        createTestUser(leaderId2);
        Party closedParty = createTestParty(15, "2024-01-15", leaderId2);
        closedParty.setPartyStatus(PartyStatus.CLOSED);
        partyDao.insertParty(closedParty);

        // when
        List<Party> parties = partyDao.findPartiesByPaymentDay(15, 31);

        // then
        assertThat(parties).anyMatch(p -> p.getPartyId().equals(activeParty.getPartyId()));
        assertThat(parties).noneMatch(p -> p.getPartyId().equals(closedParty.getPartyId()));
    }

    @Test
    @DisplayName("START_DATE가 null인 파티는 조회되지 않는다")
    void findPartiesByPaymentDay_IgnoresNullStartDate() {
        // given
        String leaderId1 = "testLeader1_" + java.util.UUID.randomUUID().toString();
        createTestUser(leaderId1);
        Party partyWithDate = createTestParty(15, "2024-01-15", leaderId1);
        partyDao.insertParty(partyWithDate);

        String leaderId2 = "testLeader2_" + java.util.UUID.randomUUID().toString();
        createTestUser(leaderId2);
        Party partyWithoutDate = createTestParty(15, null, leaderId2);
        partyDao.insertParty(partyWithoutDate);

        // when
        List<Party> parties = partyDao.findPartiesByPaymentDay(15, 31);

        // then
        assertThat(parties).anyMatch(p -> p.getPartyId().equals(partyWithDate.getPartyId()));
        assertThat(parties).noneMatch(p -> p.getPartyId().equals(partyWithoutDate.getPartyId()));
    }

    /**
     * Create test party for specified day
     *
     * @param day       Day of month (1-31)
     * @param startDate Start date string (yyyy-MM-dd) or null
     * @param leaderId  Leader User ID
     * @return Test party
     */
    private Party createTestParty(int day, String startDate, String leaderId) {
        return Party.builder()
                .productId(1)
                .partyLeaderId(leaderId)
                .maxMembers(4)
                .currentMembers(1)
                .partyStatus(PartyStatus.ACTIVE)
                .monthlyFee(10000)
                .regDate(java.time.LocalDateTime.now())
                .startDate(startDate != null ? LocalDate.parse(startDate).atStartOfDay() : null)
                .ottId("ottId")
                .ottPassword("ottPassword")
                .build();
    }

    private void createTestUser(String userId) {
        com.moa.domain.User user = com.moa.domain.User.builder()
                .userId(userId)
                .password("password")
                .nickname("nickname")
                .phone("01012345678")
                .role("USER")
                .status(com.moa.domain.enums.UserStatus.ACTIVE)
                .regDate(java.time.LocalDateTime.now())
                .build();
        userDao.insertUser(user);
    }
}
