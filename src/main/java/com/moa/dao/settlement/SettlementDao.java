package com.moa.dao.settlement;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.moa.domain.Settlement;
import com.moa.dto.settlement.response.SettlementResponse;

@Mapper
public interface SettlementDao {
	int insertSettlement(Settlement settlement);

	int updateSettlementStatus(@Param("settlementId") Integer settlementId, @Param("status") String status,
			@Param("bankTranId") String bankTranId);

	List<SettlementResponse> findByLeaderId(@Param("leaderId") String leaderId);

	Optional<Settlement> findByPartyIdAndMonth(@Param("partyId") Integer partyId,
			@Param("settlementMonth") String settlementMonth);

	Optional<Settlement> findById(@Param("settlementId") Integer settlementId);

	List<Settlement> findFailedSettlements();

	List<Settlement> findByStatus(@Param("status") String status);

	int updateStatus(@Param("settlementId") Integer settlementId, @Param("status") String status);

	int updateBankTranId(@Param("settlementId") Integer settlementId, @Param("bankTranId") String bankTranId);
}
