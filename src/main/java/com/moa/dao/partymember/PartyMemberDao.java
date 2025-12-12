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

    Optional<PartyMember> findByPartyIdAndUserId(
            @Param("partyId") Integer partyId,
            @Param("userId") String userId);

    List<PartyMemberResponse> findMembersByPartyId(@Param("partyId") Integer partyId);

    int updatePartyMember(PartyMember partyMember);

    int leaveParty(@Param("partyMemberId") Integer partyMemberId);

    /**
     * Find all active members in a party (including leader)
     * Used by PaymentScheduler for monthly payment processing
     * Returns members ordered by role (leader first) then join date
     *
     * @param partyId Party ID
     * @return List of active party members
     */
    List<PartyMember> findActiveByPartyId(@Param("partyId") Integer partyId);

    /**
     * 파티의 활성 멤버 조회 (방장 제외)
     * 월 결제 스케줄러용 - 방장은 월 구독료를 내지 않음
     *
     * @param partyId 파티 ID
     * @return 방장 제외 활성 멤버 목록
     */
    List<PartyMember> findActiveMembersExcludingLeader(@Param("partyId") Integer partyId);

    /**
     * 파티 멤버 삭제 (Toss 실패 시 롤백용)
     *
     * @param partyMemberId 파티 멤버 ID
     * @return 삭제된 행 수
     */
    int deletePartyMember(@Param("partyMemberId") Integer partyMemberId);

    /**
     * 특정 사용자의 활성 멤버십 조회 (파티장 제외)
     * 사용자 탈퇴 시 파티원으로 참여 중인 파티 처리용
     *
     * @param userId 사용자 ID
     * @return 활성 멤버십 목록
     */
    List<PartyMember> findActiveMembershipsByUserId(@Param("userId") String userId);
}