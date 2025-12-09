package com.moa.web.settlement;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moa.common.exception.ApiResponse;
import com.moa.common.exception.BusinessException;
import com.moa.common.exception.ErrorCode;
import com.moa.dto.settlement.response.SettlementDetailResponse;
import com.moa.dto.settlement.response.SettlementResponse;
import com.moa.service.settlement.SettlementService;

/**
 * 정산 관리 REST API Controller
 *
 * v1.0 구현 범위:
 * - 정산 내역 조회 (방장별)
 * - 정산 상세 내역 조회
 *
 * v1.0 제외:
 * - 수동 정산 생성 (스케줄러가 자동 처리)
 * - 정산 완료 처리 (스케줄러가 자동 처리)
 *
 * 참고:
 * - 정산은 매월 1일 자동 생성됨
 * - 방장만 자신의 정산 내역 조회 가능
 */
@RestController
@RequestMapping(value = "/api/settlements", produces = "application/json; charset=UTF-8")
public class SettlementRestController {

    private final SettlementService settlementService;

    public SettlementRestController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal == null || "anonymousUser".equals(principal)) {
            return null;
        }
        return authentication.getName();
    }

    // ========================================
    // 정산 조회
    // ========================================

    /**
     * 방장별 정산 내역 조회
     * GET /api/settlements/my
     *
     * 조회 범위:
     * - 로그인한 사용자가 방장인 파티의 모든 정산 내역
     * - 월별 정산 내역 (PENDING, COMPLETED 모두 포함)
     *
     * @return 정산 목록 (최신순)
     */
    @GetMapping("/my")
    public ApiResponse<List<SettlementResponse>> getMySettlements() {
        String userId = getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        List<SettlementResponse> response = settlementService.getSettlementsByLeaderId(userId);
        return ApiResponse.success(response);
    }

    /**
     * 정산 상세 내역 조회
     * GET /api/settlements/{settlementId}/details
     *
     * 조회 범위:
     * - 해당 정산에 포함된 모든 결제 내역
     * - 각 파티원별 결제 금액 상세
     *
     * @param settlementId 정산 ID
     * @return 정산 상세 목록 (파티원별 결제 내역)
     */
    @GetMapping("/{settlementId}/details")
    public ApiResponse<List<SettlementDetailResponse>> getSettlementDetails(
            @PathVariable Integer settlementId) {
        List<SettlementDetailResponse> response = settlementService.getSettlementDetails(settlementId);
        return ApiResponse.success(response);
    }
}
