package com.moa.service.account.impl;

import com.moa.dao.account.AccountDao;
import com.moa.dao.openbanking.AccountVerificationMapper;
import com.moa.domain.Account;
import com.moa.domain.openbanking.AccountVerification;
import com.moa.dto.openbanking.*;
import com.moa.service.account.BankAccountService;
import com.moa.service.mail.EmailService;
import com.moa.service.openbanking.MockOpenBankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import com.moa.domain.openbanking.VerificationStatus;

/**
 * 계좌 등록 및 관리 서비스 구현
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BankAccountServiceImpl implements BankAccountService {

    private final AccountDao accountDao;
    private final AccountVerificationMapper verificationMapper;
    private final MockOpenBankingService mockOpenBankingService;
    private final EmailService emailService;

    private static final int VERIFY_EXPIRY_MINUTES = 10;

    // 은행코드 → 은행명 매핑
    private String getBankName(String bankCode) {
        return switch (bankCode) {
            case "004" -> "KB국민은행";
            case "011" -> "NH농협은행";
            case "020" -> "우리은행";
            case "023" -> "SC제일은행";
            case "027" -> "한국씨티은행";
            case "081" -> "하나은행";
            case "088" -> "신한은행";
            case "089" -> "케이뱅크";
            case "090" -> "카카오뱅크";
            case "092" -> "토스뱅크";
            default -> "기타은행";
        };
    }

    // 계좌번호 마스킹 (예: 110123456789 -> 110-***-***789)
    private String maskAccountNumber(String accountNum) {
        if (accountNum == null || accountNum.length() < 6) {
            return accountNum;
        }
        int len = accountNum.length();
        String front = accountNum.substring(0, 3);
        String back = accountNum.substring(len - 3);
        return front + "-***-***" + back;
    }

    @Override
    @Transactional
    public InquiryReceiveResponse requestVerification(String userId, String bankCode, String accountNum,
            String accountHolder) {
        log.info("[계좌인증] 1원 인증 요청 - 사용자: {}, 은행: {}", userId, bankCode);

        // 4자리 인증코드 생성
        String verifyCode = mockOpenBankingService.generateVerifyCode();
        String bankTranId = "MOCK" + System.currentTimeMillis();

        // 인증 세션 저장
        AccountVerification verification = AccountVerification.builder()
                .userId(userId)
                .bankTranId(bankTranId)
                .bankCode(bankCode)
                .accountNum(accountNum)
                .accountHolder(accountHolder)
                .verifyCode(verifyCode)
                .attemptCount(0)
                .status(VerificationStatus.PENDING)
                .expiredAt(LocalDateTime.now().plusMinutes(VERIFY_EXPIRY_MINUTES))
                .build();

        verificationMapper.insert(verification);

        log.info("[계좌인증] 인증 세션 생성 완료 - 거래ID: {}, 인증코드: {}", bankTranId, verifyCode);

        // 이메일로 인증코드 발송 (userId가 이메일 주소)
        String bankName = getBankName(bankCode);
        String maskedAccount = maskAccountNumber(accountNum);
        emailService.sendBankVerificationEmail(userId, bankName, maskedAccount, verifyCode);

        LocalDateTime expiresAt = verification.getExpiredAt();
        return InquiryReceiveResponse.success(bankTranId, verifyCode, maskedAccount, expiresAt);
    }

    @Override
    @Transactional
    public InquiryVerifyResponse verifyAndRegister(String userId, String bankTranId, String verifyCode) {
        log.info("[계좌인증] 인증코드 검증 - 사용자: {}, 거래ID: {}", userId, bankTranId);

        // 인증 세션 조회
        AccountVerification verification = verificationMapper.findByBankTranId(bankTranId);

        if (verification == null) {
            log.warn("[계좌인증] 인증 실패 - 세션 없음 (ID: {})", bankTranId);
            return InquiryVerifyResponse.fail("A0004", "인증 세션을 찾을 수 없습니다");
        }

        // 사용자 확인
        if (!verification.getUserId().equals(userId)) {
            log.warn("[계좌인증] 인증 실패 - 사용자 불일치 (Req: {}, DB: {})", userId, verification.getUserId());
            return InquiryVerifyResponse.fail("A0004", "잘못된 인증 요청입니다");
        }

        // 만료 확인
        if (LocalDateTime.now().isAfter(verification.getExpiredAt())) {
            verificationMapper.updateStatus(verification.getVerificationId(), "EXPIRED");
            log.warn("[계좌인증] 인증 실패 - 세션 만료");
            return InquiryVerifyResponse.fail("A0004", "인증 세션이 만료되었습니다");
        }

        // 이미 처리된 세션 확인
        if (VerificationStatus.PENDING != verification.getStatus()) {
            log.warn("[계좌인증] 인증 실패 - 상태 오류 (Current: {})", verification.getStatus());
            return InquiryVerifyResponse.fail("A0004", "이미 처리된 인증 세션입니다");
        }

        // 시도 횟수 증가
        verificationMapper.incrementAttemptCount(verification.getVerificationId());
        int newAttemptCount = verification.getAttemptCount() + 1;

        // 인증코드 비교
        if (!verification.getVerifyCode().equals(verifyCode)) {
            if (newAttemptCount >= 3) {
                verificationMapper.updateStatus(verification.getVerificationId(), "FAILED");
                log.warn("[계좌인증] 인증 실패 - 시도횟수 초과");
                return InquiryVerifyResponse.fail("A0005", "인증 시도 횟수를 초과했습니다");
            }
            log.warn("[계좌인증] 코드 불일치 - DB: [{}], Input: [{}]", verification.getVerifyCode(), verifyCode);
            return InquiryVerifyResponse.fail("A0003", "인증코드가 일치하지 않습니다");
        }

        // 인증 성공 - 핀테크이용번호 생성
        String fintechUseNum = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        verificationMapper.updateStatus(verification.getVerificationId(), "VERIFIED");

        // 기존 계좌 비활성화
        Optional<Account> existingAccount = accountDao.findActiveByUserId(userId);
        existingAccount.ifPresent(account -> {
            accountDao.updateStatus(account.getAccountId(), "INACTIVE");
            log.info("[계좌인증] 기존 계좌 비활성화 - accountId: {}", account.getAccountId());
        });

        // 새 계좌 등록
        Account newAccount = Account.builder()
                .userId(userId)
                .bankCode(verification.getBankCode())
                .bankName(getBankName(verification.getBankCode()))
                .accountNumber(verification.getAccountNum())
                .accountHolder(verification.getAccountHolder())
                .fintechUseNum(fintechUseNum)
                .status("ACTIVE")
                .isVerified("Y")
                .build();

        accountDao.insertAccount(newAccount);

        log.info("[계좌인증] 계좌 등록 완료 - 사용자: {}, 핀테크번호: {}", userId, fintechUseNum);

        return InquiryVerifyResponse.success(fintechUseNum);
    }

    @Override
    public Account getAccount(String userId) {
        return accountDao.findActiveByUserId(userId).orElse(null);
    }

    @Override
    @Transactional
    public void deleteAccount(String userId) {
        log.info("[계좌관리] 계좌 삭제 요청 - 사용자: {}", userId);

        Optional<Account> account = accountDao.findActiveByUserId(userId);
        account.ifPresent(acc -> {
            accountDao.updateStatus(acc.getAccountId(), "INACTIVE");
            log.info("[계좌관리] 계좌 비활성화 완료 - accountId: {}", acc.getAccountId());
        });
    }

    @Override
    @Transactional
    public InquiryReceiveResponse changeAccount(String userId, String bankCode, String accountNum,
            String accountHolder) {
        log.info("[계좌관리] 계좌 변경 요청 - 사용자: {}", userId);

        // 새 계좌 인증 시작 (기존 계좌는 인증 완료 시 비활성화됨)
        return requestVerification(userId, bankCode, accountNum, accountHolder);
    }
}
