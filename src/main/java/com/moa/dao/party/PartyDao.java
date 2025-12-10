package com.moa.dao.party;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.moa.domain.Party;
import com.moa.domain.enums.PartyStatus;
import com.moa.dto.party.response.PartyDetailResponse;
import com.moa.dto.party.response.PartyListResponse;

@Mapper
public interface PartyDao {

	int insertParty(Party party);

	Optional<Party> findById(@Param("partyId") Integer partyId);

	Optional<PartyDetailResponse> findDetailById(@Param("partyId") Integer partyId);

	List<PartyListResponse> findPartyList(
			@Param("productId") Integer productId,
			@Param("partyStatus") PartyStatus partyStatus,
			@Param("keyword") String keyword,
			@Param("startDate") java.time.LocalDate startDate,
			@Param("offset") int offset,
			@Param("size") int size,
			@Param("sort") String sort);

	int updateParty(Party party);

	int updateOttAccount(
			@Param("partyId") Integer partyId,
			@Param("ottId") String ottId,
			@Param("ottPassword") String ottPassword);

	int updatePartyStatus(
			@Param("partyId") Integer partyId,
			@Param("status") PartyStatus status);

	int incrementCurrentMembers(@Param("partyId") Integer partyId);

	int decrementCurrentMembers(@Param("partyId") Integer partyId);

	List<PartyListResponse> findMyParties(@Param("userId") String userId);

	List<PartyListResponse> findMyLeadingParties(@Param("userId") String userId);

	List<PartyListResponse> findMyParticipatingParties(@Param("userId") String userId);

	List<Party> findActiveParties();

	/**
	 * Find active parties by payment day (supports edge cases for 29/30/31)
	 * Used by PaymentScheduler to determine which parties need payment today
	 *
	 * Edge case handling:
	 * - If party starts on 31st and today is Feb 28 (last day), party is included
	 * - If party starts on 30th and today is Feb 28 (last day), party is included
	 *
	 * @param currentDay     Day of month today (1-31)
	 * @param lastDayOfMonth Last day of current month (28-31)
	 * @return List of active parties needing payment today
	 */
	List<Party> findPartiesByPaymentDay(
			@Param("currentDay") int currentDay,
			@Param("lastDayOfMonth") int lastDayOfMonth);

	long countAllParties();

	long countActiveParties();

	List<Party> findExpiredActiveParties(@Param("now") java.time.LocalDateTime now);

	List<Party> findExpiredPendingPaymentParties(
			@Param("status") PartyStatus status,
			@Param("timeoutThreshold") java.time.LocalDateTime timeoutThreshold);
}