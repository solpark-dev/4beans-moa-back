package com.moa.domain.openbanking;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private VerificationStatus status;
    private LocalDateTime expiredAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
