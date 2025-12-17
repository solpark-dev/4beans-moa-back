package com.moa.service.openbanking;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.UUID;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moa.dao.openbanking.AccountVerificationMapper;
import com.moa.dao.openbanking.TransferTransactionMapper;
import com.moa.domain.openbanking.AccountVerification;
import com.moa.domain.openbanking.TransactionStatus;
import com.moa.domain.openbanking.TransferTransaction;
import com.moa.domain.openbanking.VerificationStatus;
import com.moa.dto.openbanking.InquiryReceiveRequest;
import com.moa.dto.openbanking.InquiryReceiveResponse;
import com.moa.dto.openbanking.InquiryVerifyRequest;
import com.moa.dto.openbanking.InquiryVerifyResponse;
import com.moa.dto.openbanking.TransferDepositRequest;
import com.moa.dto.openbanking.TransferDepositResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class MockOpenBankingService implements OpenBankingClient {

    private final AccountVerificationMapper verificationMapper;
    private final TransferTransactionMapper transactionMapper;

    private static final int VERIFY_CODE_LENGTH = 4;
    private static final int VERIFY_EXPIRY_MINUTES = 5;
    private static final int MAX_VERIFY_ATTEMPTS = 3;

    @Override
    @Transactional
    public InquiryReceiveResponse requestVerification(InquiryReceiveRequest request) {
        // 필수 파라미터 검증
        if (request.getBankCodeStd() == null || request.getBankCodeStd().isBlank()) {
            return InquiryReceiveResponse.error("A0001", "필수 파라미터가 누락되었습니다: bankCodeStd");
        }
        if (request.getAccountNum() == null || request.getAccountNum().isBlank()) {
            return InquiryReceiveResponse.error("A0001", "필수 파라미터가 누락되었습니다: accountNum");
        }
        if (request.getAccountHolderInfo() == null || request.getAccountHolderInfo().isBlank()) {
            return InquiryReceiveResponse.error("A0001", "필수 파라미터가 누락되었습니다: accountHolderInfo");
        }

        String verifyCode = generateVerifyCode();
        String bankTranId = generateBankTranId();

        log.info("[Mock] 1원 인증 처리 완료 - 거래ID: {}, 인증코드: {}", bankTranId, verifyCode);

        return InquiryReceiveResponse.success(bankTranId, verifyCode);
    }

    @Transactional
    public InquiryReceiveResponse processInquiryReceiveWithUser(String userId, InquiryReceiveRequest request) {
        InquiryReceiveResponse basicResponse = requestVerification(request);
        if (!"A0000".equals(basicResponse.getRspCode())) {
            return basicResponse;
        }
        String verifyCode = generateVerifyCode();
        String bankTranId = generateBankTranId();
        AccountVerification verification = AccountVerification.builder()
                .userId(userId)
                .bankTranId(bankTranId)
                .bankCode(request.getBankCodeStd())
                .accountNum(request.getAccountNum())
                .accountHolder(request.getAccountHolderInfo())
                .verifyCode(verifyCode)
                .attemptCount(0)
                .status(VerificationStatus.PENDING)
                .expiredAt(LocalDateTime.now().plusMinutes(VERIFY_EXPIRY_MINUTES))
                .build();

        verificationMapper.insert(verification);

        log.info("[Mock] 1원 인증 세션 생성 - 사용자: {}, 거래ID: {}", userId, bankTranId);

        return InquiryReceiveResponse.success(bankTranId, verifyCode);
    }

    @Override
    @Transactional
    public InquiryVerifyResponse verifyCode(InquiryVerifyRequest request) {
        if (request.getBankTranId() == null || request.getBankTranId().isBlank()) {
            return InquiryVerifyResponse.fail("A0001", "필수 파라미터가 누락되었습니다: bankTranId");
        }
        if (request.getVerifyCode() == null || request.getVerifyCode().isBlank()) {
            return InquiryVerifyResponse.fail("A0001", "필수 파라미터가 누락되었습니다: verifyCode");
        }
        AccountVerification verification = verificationMapper.findByBankTranId(request.getBankTranId());

        if (verification == null) {
            return InquiryVerifyResponse.fail("A0004", "인증 세션을 찾을 수 없습니다");
        }

        if (LocalDateTime.now().isAfter(verification.getExpiredAt())) {
            verificationMapper.updateStatus(verification.getVerificationId(), "EXPIRED");
            return InquiryVerifyResponse.fail("A0004", "인증 세션이 만료되었습니다");
        }

        if (!"PENDING".equals(verification.getStatus())) {
            return InquiryVerifyResponse.fail("A0004", "이미 처리된 인증 세션입니다");
        }

        int newAttemptCount = verification.getAttemptCount() + 1;
        verificationMapper.incrementAttemptCount(verification.getVerificationId());

        if (!verification.getVerifyCode().equals(request.getVerifyCode())) {
            if (newAttemptCount >= MAX_VERIFY_ATTEMPTS) {
                verificationMapper.updateStatus(verification.getVerificationId(), "FAILED");
                return InquiryVerifyResponse.fail("A0005", "인증 시도 횟수를 초과했습니다");
            }
            return InquiryVerifyResponse.fail("A0003", "인증코드가 일치하지 않습니다");
        }

        String fintechUseNum = generateFintechUseNum();
        verificationMapper.updateStatus(verification.getVerificationId(), "VERIFIED");

        log.info("[Mock] 인증 성공 - 거래ID: {}, 핀테크번호: {}", request.getBankTranId(), fintechUseNum);

        return InquiryVerifyResponse.success(fintechUseNum);
    }

    @Override
    @Transactional
    public TransferDepositResponse transferDeposit(TransferDepositRequest request) {
        if (request.getFintechUseNum() == null || request.getFintechUseNum().isBlank()) {
            return TransferDepositResponse.error("A0001", "필수 파라미터가 누락되었습니다: fintechUseNum");
        }
        if (request.getTranAmt() == null || request.getTranAmt() <= 0) {
            return TransferDepositResponse.error("A0002", "이체금액이 올바르지 않습니다");
        }

        String bankTranId = generateBankTranId();

        log.info("[Mock] 입금이체 처리 완료 - 거래ID: {}, 금액: {}", bankTranId, request.getTranAmt());

        return TransferDepositResponse.success(bankTranId, request.getTranAmt());
    }

    @Transactional
    public TransferDepositResponse transferDepositWithSettlement(Integer settlementId, TransferDepositRequest request) {
        TransferDepositResponse basicResponse = transferDeposit(request);
        if (!"A0000".equals(basicResponse.getRspCode())) {
            TransferTransaction transaction = TransferTransaction.builder()
                    .settlementId(settlementId)
                    .bankTranId(generateBankTranId())
                    .fintechUseNum(request.getFintechUseNum())
                    .tranAmt(request.getTranAmt())
                    .printContent(request.getPrintContent())
                    .reqClientName(request.getReqClientName())
                    .rspCode(basicResponse.getRspCode())
                    .rspMessage(basicResponse.getRspMessage())
                    .status(TransactionStatus.FAILED)
                    .build();
            transactionMapper.insert(transaction);
            return basicResponse;
        }
        TransferTransaction transaction = TransferTransaction.builder()
                .settlementId(settlementId)
                .bankTranId(basicResponse.getBankTranId())
                .fintechUseNum(request.getFintechUseNum())
                .tranAmt(request.getTranAmt())
                .printContent(request.getPrintContent())
                .reqClientName(request.getReqClientName())
                .rspCode("A0000")
                .rspMessage("이체 성공")
                .status(TransactionStatus.SUCCESS)
                .build();
        transactionMapper.insert(transaction);

        return basicResponse;
    }

    public String generateVerifyCode() {
        Random random = new Random();
        int code = 1000 + random.nextInt(9000);
        return String.valueOf(code);
    }

    private String generateBankTranId() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.format("%04d", new Random().nextInt(10000));
        return "MOCK" + timestamp + random;
    }

    private String generateFintechUseNum() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 24);
    }
}
