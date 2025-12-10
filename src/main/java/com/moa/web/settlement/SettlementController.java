package com.moa.web.settlement;

import com.moa.common.exception.ApiResponse;

import com.moa.dto.settlement.response.SettlementResponse;
import com.moa.dto.settlement.response.SettlementDetailResponse;
import com.moa.service.settlement.SettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 정산 내역 조회 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    /**
     * 정산 내역 조회 (기간 필터 지원)
     * GET /api/settlements?startDate=2024-01-01&endDate=2024-12-31
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<SettlementResponse>>> getSettlements(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        String userId = userDetails.getUsername();
        log.info("[API] 정산 내역 조회 - 사용자: {}, 기간: {} ~ {}", userId, startDate, endDate);

        // Service only supports getSettlementsByLeaderId currently. Date filtering
        // ignored for now as per conflict resolution plan.
        List<SettlementResponse> settlements = settlementService.getSettlementsByLeaderId(userId);

        return ResponseEntity.ok(ApiResponse.success(settlements));
    }

    /**
     * 정산 상세 내역 조회 (포함된 결제 목록)
     * GET /api/settlements/{settlementId}/details
     */
    @GetMapping("/{settlementId}/details")
    public ResponseEntity<ApiResponse<List<SettlementDetailResponse>>> getSettlementDetails(
            @PathVariable Integer settlementId) {

        log.info("[API] 정산 상세 조회 - settlementId: {}", settlementId);

        List<SettlementDetailResponse> payments = settlementService.getSettlementDetails(settlementId);

        return ResponseEntity.ok(ApiResponse.success(payments));
    }
}
