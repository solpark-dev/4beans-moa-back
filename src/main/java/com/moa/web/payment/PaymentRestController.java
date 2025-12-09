package com.moa.web.payment;

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
import com.moa.dto.payment.response.PaymentDetailResponse;
import com.moa.dto.payment.response.PaymentResponse;
import com.moa.service.payment.PaymentService;

/**
 * 결제 관리 REST API Controller
 *
 * v1.0 구현 범위:
 * - 결제 내역 조회 (상세/목록)
 * - 사용자별 결제 내역 조회
 * - 파티별 결제 내역 조회
 *
 * v1.0 제외:
 * - 수동 결제 생성 (파티 생성/가입 시 자동 처리)
 * - 결제 취소/환불 (v2.0)
 * - 결제 실패 재시도 (v2.0)
 *
 * 참고:
 * - 방장 보증금 결제: /api/parties/{partyId}/leader-deposit
 * - 파티원 통합 결제: /api/parties/{partyId}/join
 */
@RestController
@RequestMapping(value = "/api/v1/payments", produces = "application/json; charset=UTF-8")
public class PaymentRestController {

    private final PaymentService paymentService;

    public PaymentRestController(PaymentService paymentService) {
        this.paymentService = paymentService;
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
    // 결제 조회
    // ========================================

    /**
     * 결제 상세 조회
     * GET /api/payments/{paymentId}
     *
     * @param paymentId 결제 ID
     * @return 결제 상세 정보 (카드 정보, 파티 정보 포함)
     */
    @GetMapping("/{paymentId}")
    public ApiResponse<PaymentDetailResponse> getPaymentDetail(@PathVariable Integer paymentId) {
        PaymentDetailResponse response = paymentService.getPaymentDetail(paymentId);
        return ApiResponse.success(response);
    }

    /**
     * 내 결제 내역 조회
     * GET /api/payments/my
     *
     * 조회 범위:
     * - 파티원 첫 달 결제
     * - 월별 자동 결제
     *
     * @return 내 결제 목록 (최신순)
     */
    @GetMapping("/my")
    public ApiResponse<List<PaymentResponse>> getMyPayments() {
        String userId = getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        List<PaymentResponse> response = paymentService.getMyPayments(userId);
        return ApiResponse.success(response);
    }

    /**
     * 파티별 결제 내역 조회
     * GET /api/payments/party/{partyId}
     *
     * 조회 범위:
     * - 해당 파티의 모든 멤버 결제 내역
     * - 첫 달 결제 + 월별 자동 결제
     *
     * 참고:
     * - 방장/멤버 모두 조회 가능
     * - 보증금 내역은 /api/deposits/party/{partyId}에서 조회
     *
     * @param partyId 파티 ID
     * @return 파티 결제 목록 (최신순)
     */
    @GetMapping("/party/{partyId}")
    public ApiResponse<List<PaymentResponse>> getPartyPayments(@PathVariable Integer partyId) {
        List<PaymentResponse> response = paymentService.getPartyPayments(partyId);
        return ApiResponse.success(response);
    }
}