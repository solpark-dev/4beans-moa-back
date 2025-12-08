package com.moa.dao.settlement;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.moa.domain.SettlementDetail;
import com.moa.dto.settlement.response.SettlementDetailResponse;

@Mapper
public interface SettlementDetailDao {
    int insertSettlementDetail(SettlementDetail detail);

    List<SettlementDetailResponse> findBySettlementId(@Param("settlementId") Integer settlementId);
}
