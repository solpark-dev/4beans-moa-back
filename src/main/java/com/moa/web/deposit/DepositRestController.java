package com.moa.web.deposit;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moa.common.exception.ApiResponse;
import com.moa.dto.deposit.response.DepositResponse;
import com.moa.service.deposit.DepositService;

/**
 * 보증금 관리 REST API Controller
 *
 * v1.0 구현 범위:
 * - 보증금 내역 조회 (상세/목록)
 * - 사용자별 보증금 내역 조회
 * - 파티별 보증금 내역 조회
 *
 * v1.0 제외:
 * - 수동 보증금 생성 (파티 생성/가입 시 자동 처리)
 * - 보증금 환불 (v2.0)
 * - 보증금 몰수 처리 (v2.0)
 *
 * 참고:
 * - 방장 보증금 결제: /api/parties/{partyId}/leader-deposit
 * - 파티원 보증금 결제: /api/parties/{partyId}/join (통합 결제)
 */
@RestController
@RequestMapping(value = "/api/deposits", produces = "application/json; charset=UTF-8")
public class DepositRestController {

    private final DepositService depositService;

    public DepositRestController(DepositService depositService) {
        this.depositService = depositService;
    }

    private String getCurrentUserId() {
        org.springframework.security.core.Authentication authentication = 
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
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
    // 보증금 조회
    // ========================================

    /**
     * 보증금 상세 조회
     * GET /api/deposits/{depositId}
     *
     * @param depositId 보증금 ID
     * @return 보증금 상세 정보 (파티 정보, 사용자 정보 포함)
     */
    @GetMapping("/{depositId}")
    public ApiResponse<DepositResponse> getDepositDetail(@PathVariable Integer depositId) {
        DepositResponse response = depositService.getDepositDetail(depositId);
        return ApiResponse.success(response);
    }

    /**
     * 내 보증금 내역 조회
     * GET /api/deposits/my
     *
     * 조회 범위:
     * - 방장으로 납부한 보증금
     * - 파티원으로 납부한 보증금
     *
     * @return 내 보증금 목록 (최신순)
     */
    @GetMapping("/my")
    public ApiResponse<List<DepositResponse>> getMyDeposits() {
        String userId = getCurrentUserId();
        if (userId == null) {
            throw new com.moa.common.exception.BusinessException(
                com.moa.common.exception.ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        List<DepositResponse> response = depositService.getMyDeposits(userId);
        return ApiResponse.success(response);
    }

    /**
     * 파티별 보증금 내역 조회
     * GET /api/deposits/party/{partyId}
     *
     * 조회 범위:
     * - 해당 파티의 모든 멤버 보증금 내역
     * - 방장 보증금 + 파티원 보증금
     *
     * 참고:
     * - 방장/멤버 모두 조회 가능
     * - 월별 구독료 결제는 /api/payments/party/{partyId}에서 조회
     *
     * @param partyId 파티 ID
     * @return 파티 보증금 목록 (최신순)
     */
    @GetMapping("/party/{partyId}")
    public ApiResponse<List<DepositResponse>> getPartyDeposits(@PathVariable Integer partyId) {
        List<DepositResponse> response = depositService.getPartyDeposits(partyId);
        return ApiResponse.success(response);
    }
}