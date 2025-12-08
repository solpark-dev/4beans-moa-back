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
}