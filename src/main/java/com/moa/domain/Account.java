package com.moa.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {
    private Integer accountId;
    private String userId;
    private String bankCode;
    private String bankName;
    private String accountNumber;
    private String accountHolder;
    private String isVerified; // 'Y' or 'N'
    private LocalDateTime regDate;
    private LocalDateTime verifyDate;
}
