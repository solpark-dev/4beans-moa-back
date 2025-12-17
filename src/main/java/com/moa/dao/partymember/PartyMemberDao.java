package com.moa.dao.partymember;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.moa.domain.PartyMember;
import com.moa.dto.partymember.response.PartyMemberResponse;

@Mapper
public interface PartyMemberDao {

	int insertPartyMember(PartyMember partyMember);

	Optional<PartyMemberResponse> findByPartyMemberId(@Param("partyMemberId") Integer partyMemberId);

	Optional<PartyMember> findByPartyIdAndUserId(@Param("partyId") Integer partyId, @Param("userId") String userId);

	List<PartyMemberResponse> findMembersByPartyId(@Param("partyId") Integer partyId);

	int updatePartyMember(PartyMember partyMember);

	int leaveParty(@Param("partyMemberId") Integer partyMemberId);

	List<PartyMember> findActiveByPartyId(@Param("partyId") Integer partyId);

	List<PartyMember> findActiveMembersExcludingLeader(@Param("partyId") Integer partyId);

	int deletePartyMember(@Param("partyMemberId") Integer partyMemberId);

	List<PartyMember> findActiveMembershipsByUserId(@Param("userId") String userId);
}