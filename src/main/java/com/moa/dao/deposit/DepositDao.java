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

	Optional<Deposit> findByPartyIdAndUserId(@Param("partyId") Integer partyId, @Param("userId") String userId);

	int updateDeposit(Deposit deposit);

	List<Deposit> findForfeitedByPartyIdAndPeriod(@Param("partyId") Integer partyId,
			@Param("startDate") java.time.LocalDateTime startDate, @Param("endDate") java.time.LocalDateTime endDate);

	int deleteById(@Param("depositId") Integer depositId);

	int deleteStalePendingRecords(@Param("cutoffTime") java.time.LocalDateTime cutoffTime);
}