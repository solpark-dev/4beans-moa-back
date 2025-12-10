package com.moa.domain.openbanking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 1원 인증 세션 도메인
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountVerification {

    private Long verificationId;
    private String userId;
    private String bankTranId;
    private String bankCode;
    private String accountNum;
    private String accountHolder;
    private String verifyCode;
    private Integer attemptCount;
    private VerificationStatus status; // PENDING, VERIFIED, EXPIRED, FAILED
    private LocalDateTime expiredAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
