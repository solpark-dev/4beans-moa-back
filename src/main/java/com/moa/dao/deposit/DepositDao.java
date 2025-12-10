package com.moa.dao.deposit;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.moa.domain.Deposit;
import com.moa.dto.deposit.response.DepositResponse;

@Mapper
public interface DepositDao {

    int insertDeposit(Deposit deposit);

    Optional<Deposit> findById(@Param("depositId") Integer depositId);

    Optional<DepositResponse> findDetailById(@Param("depositId") Integer depositId);

    Optional<Deposit> findByPartyMemberId(@Param("partyMemberId") Integer partyMemberId);

    List<DepositResponse> findByUserId(@Param("userId") String userId);

    List<DepositResponse> findByPartyId(@Param("partyId") Integer partyId);

    /**
     * 파티 ID와 사용자 ID로 보증금 조회
     *
     * @param partyId 파티 ID
     * @param userId  사용자 ID
     * @return 보증금 (PAID 상태만)
     */
    Optional<Deposit> findByPartyIdAndUserId(@Param("partyId") Integer partyId, @Param("userId") String userId);

    int updateDeposit(Deposit deposit);

    /**
     * 정산 기간 내 몰수된 보증금 조회
     *
     * @param partyId   파티 ID
     * @param startDate 기간 시작일
     * @param endDate   기간 종료일
     * @return 몰수된 보증금 목록
     */
    List<Deposit> findForfeitedByPartyIdAndPeriod(
            @Param("partyId") Integer partyId,
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate);

    /**
     * 보증금 삭제 (PENDING 상태 정리용)
     *
     * @param depositId 보증금 ID
     * @return 삭제된 행 수
     */
    int deleteById(@Param("depositId") Integer depositId);

    /**
     * 오래된 PENDING 상태 보증금 삭제
     *
     * @param cutoffTime 기준 시간 (이 시간 이전에 생성된 PENDING 상태 삭제)
     * @return 삭제된 행 수
     */
    int deleteStalePendingRecords(@Param("cutoffTime") java.time.LocalDateTime cutoffTime);
}