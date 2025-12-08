package com.moa.web.openbanking;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.moa.common.exception.ApiResponse;
import com.moa.common.exception.BusinessException;
import com.moa.common.exception.ErrorCode;
import com.moa.service.openbanking.OpenBankingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 오픈뱅킹 계좌 등록 REST API Controller
 * 
 * 기능:
 * - 오픈뱅킹 OAuth 인증 URL 생성
 * - OAuth 콜백 처리
 * - 1원 인증 검증
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/openbanking", produces = "application/json; charset=UTF-8")
@RequiredArgsConstructor
public class OpenBankingRestController {

    private final OpenBankingService openBankingService;

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

    /**
     * 오픈뱅킹 인증 URL 생성
     * GET /api/openbanking/auth-url
     */
    @GetMapping("/auth-url")
    public ApiResponse<Map<String, String>> getAuthUrl() {
        String userId = getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        String authUrl = openBankingService.getAuthorizationUrl(userId);
        return ApiResponse.success(Map.of("authUrl", authUrl));
    }

    /**
     * 오픈뱅킹 OAuth 콜백
     * GET /api/openbanking/callback
     * 
     * 오픈뱅킹에서 인증 완료 후 리다이렉트됨
     */
    @GetMapping("/callback")
    public String handleCallback(
            @RequestParam("code") String code,
            @RequestParam("state") String state) {

        try {
            // state에서 userId 추출
            String userId = state;

            // Access Token 발급 및 계좌 정보 저장
            openBankingService.processCallback(userId, code);

            // 프론트엔드 1원 인증 페이지로 리다이렉트
            return "redirect:http://localhost:5173/user/account-verify";
        } catch (Exception e) {
            log.error("OpenBanking callback error", e);
            return "redirect:http://localhost:5173/user/my-wallet?error=openbanking_failed";
        }
    }

    /**
     * 1원 인증 시작 (1원 입금)
     * POST /api/openbanking/send-verification
     */
    @PostMapping("/send-verification")
    public ApiResponse<Map<String, String>> sendVerification() {
        String userId = getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        // 실제로는 코드를 반환하지 않음 (사용자가 통장에서 확인)
        // 테스트 환경에서만 코드 반환
        return ApiResponse.success(Map.of("message", "1원이 입금되었습니다. 입금자명을 확인해주세요."));
    }

    /**
     * 1원 인증 검증
     * POST /api/openbanking/verify
     */
    @PostMapping("/verify")
    public ApiResponse<Map<String, Object>> verifyAccount(@RequestBody Map<String, String> request) {
        String userId = getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        String inputCode = request.get("code");
        if (inputCode == null || inputCode.length() != 4) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "4자리 인증 코드를 입력해주세요.");
        }

        boolean verified = openBankingService.verifyAccount(userId, inputCode);

        if (verified) {
            return ApiResponse.success(Map.of(
                    "verified", true,
                    "message", "계좌 인증이 완료되었습니다."));
        } else {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "인증 코드가 일치하지 않습니다.");
        }
    }
}
