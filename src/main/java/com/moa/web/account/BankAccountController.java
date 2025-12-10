package com.moa.web.account;

import com.moa.common.exception.ApiResponse;
import com.moa.common.exception.ErrorCode;
import com.moa.domain.Account;
import com.moa.dto.openbanking.InquiryReceiveResponse;
import com.moa.dto.openbanking.InquiryVerifyResponse;
import com.moa.service.account.BankAccountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * 계좌 등록 및 관리 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/bank-account")
@RequiredArgsConstructor
public class BankAccountController {
    
    private final BankAccountService bankAccountService;
    
    /**
     * 1원 인증 요청
     * POST /api/bank-account/verify-request
     */
    @PostMapping("/verify-request")
    public ResponseEntity<ApiResponse<InquiryReceiveResponse>> requestVerification(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody VerifyRequestDto request) {
        
        String userId = userDetails.getUsername();
        log.info("[API] 1원 인증 요청 - 사용자: {}", userId);
        
        InquiryReceiveResponse response = bankAccountService.requestVerification(
                userId,
                request.getBankCode(),
                request.getAccountNum(),
                request.getAccountHolder()
        );
        
        if ("A0000".equals(response.getRspCode())) {
            return ResponseEntity.ok(ApiResponse.success(response));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error(ErrorCode.BAD_REQUEST, response.getRspMessage()));
        }
    }
    
    /**
     * 인증코드 검증 및 계좌 등록
     * POST /api/bank-account/verify
     */
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<InquiryVerifyResponse>> verifyAndRegister(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody VerifyCodeDto request) {
        
        String userId = userDetails.getUsername();
        log.info("[API] 인증코드 검증 - 사용자: {}", userId);
        
        InquiryVerifyResponse response = bankAccountService.verifyAndRegister(
                userId,
                request.getBankTranId(),
                request.getVerifyCode()
        );
        
        if (response.isVerified()) {
            return ResponseEntity.ok(ApiResponse.success(response));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error(ErrorCode.BAD_REQUEST, response.getRspMessage()));
        }
    }
    
    /**
     * 계좌 조회
     * GET /api/bank-account
     */
    @GetMapping
    public ResponseEntity<ApiResponse<AccountResponseDto>> getAccount(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        String userId = userDetails.getUsername();
        Account account = bankAccountService.getAccount(userId);
        
        if (account == null) {
            return ResponseEntity.ok(ApiResponse.success(null));
        }
        
        AccountResponseDto response = AccountResponseDto.from(account);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 계좌 삭제
     * DELETE /api/bank-account
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        String userId = userDetails.getUsername();
        log.info("[API] 계좌 삭제 - 사용자: {}", userId);
        
        bankAccountService.deleteAccount(userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
    
    /**
     * 계좌 변경 (새 인증 시작)
     * POST /api/bank-account/change
     */
    @PostMapping("/change")
    public ResponseEntity<ApiResponse<InquiryReceiveResponse>> changeAccount(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody VerifyRequestDto request) {
        
        String userId = userDetails.getUsername();
        log.info("[API] 계좌 변경 요청 - 사용자: {}", userId);
        
        InquiryReceiveResponse response = bankAccountService.changeAccount(
                userId,
                request.getBankCode(),
                request.getAccountNum(),
                request.getAccountHolder()
        );
        
        if ("A0000".equals(response.getRspCode())) {
            return ResponseEntity.ok(ApiResponse.success(response));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error(ErrorCode.BAD_REQUEST, response.getRspMessage()));
        }
    }
    
    // DTO 클래스들
    
    @Data
    public static class VerifyRequestDto {
        @NotBlank(message = "은행코드는 필수입니다")
        @Size(min = 3, max = 3, message = "은행코드는 3자리입니다")
        private String bankCode;
        
        @NotBlank(message = "계좌번호는 필수입니다")
        private String accountNum;
        
        @NotBlank(message = "예금주명은 필수입니다")
        private String accountHolder;
    }
    
    @Data
    public static class VerifyCodeDto {
        @NotBlank(message = "거래고유번호는 필수입니다")
        private String bankTranId;
        
        @NotBlank(message = "인증코드는 필수입니다")
        @Size(min = 4, max = 4, message = "인증코드는 4자리입니다")
        private String verifyCode;
    }
    
    @Data
    public static class AccountResponseDto {
        private Integer accountId;
        private String bankCode;
        private String bankName;
        private String maskedAccountNumber;
        private String accountHolder;
        private String status;
        
        public static AccountResponseDto from(Account account) {
            AccountResponseDto dto = new AccountResponseDto();
            dto.setAccountId(account.getAccountId());
            dto.setBankCode(account.getBankCode());
            dto.setBankName(account.getBankName());
            dto.setMaskedAccountNumber(account.getMaskedAccountNumber());
            dto.setAccountHolder(account.getAccountHolder());
            dto.setStatus(account.getStatus());
            return dto;
        }
    }
}
