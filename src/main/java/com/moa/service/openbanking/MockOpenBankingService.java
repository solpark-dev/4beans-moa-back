package com.moa.service.openbanking;

import com.moa.dto.openbanking.*;
import com.moa.dao.openbanking.AccountVerificationMapper;
import com.moa.dao.openbanking.TransferTransactionMapper;
import com.moa.domain.openbanking.AccountVerification;
import com.moa.domain.openbanking.TransferTransaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.UUID;

import com.moa.domain.openbanking.VerificationStatus;
import com.moa.domain.openbanking.TransactionStatus;

/**
 * Mock 오픈뱅킹 서비스
 * 실제 오픈뱅킹 API 동작을 시뮬레이션
 */
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

    /**
     * 1원 인증 요청 처리 (수취조회)
     * - 4자리 인증코드 생성
     * - 거래고유번호 생성
     * - 인증 세션 저장
     */
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

        // 4자리 인증코드 생성
        String verifyCode = generateVerifyCode();

        // 거래고유번호 생성 (MOCK + 날짜시간 + 랜덤)
        String bankTranId = generateBankTranId();

        // 인증 세션 저장 (실제로는 사용자 ID가 필요하지만, Mock에서는 임시로 처리)
        // 실제 구현에서는 BankAccountService에서 userId와 함께 호출

        log.info("[Mock] 1원 인증 처리 완료 - 거래ID: {}, 인증코드: {}", bankTranId, verifyCode);

        return InquiryReceiveResponse.success(bankTranId, verifyCode);
    }

    /**
     * 1원 인증 요청 처리 (사용자 ID 포함)
     */
    @Transactional
    public InquiryReceiveResponse processInquiryReceiveWithUser(String userId, InquiryReceiveRequest request) {
        // 기본 검증
        InquiryReceiveResponse basicResponse = requestVerification(request);
        if (!"A0000".equals(basicResponse.getRspCode())) {
            return basicResponse;
        }

        // 4자리 인증코드 생성
        String verifyCode = generateVerifyCode();
        String bankTranId = generateBankTranId();

        // 인증 세션 저장
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

    /**
     * 인증코드 검증
     */
    @Override
    @Transactional
    public InquiryVerifyResponse verifyCode(InquiryVerifyRequest request) {
        // 필수 파라미터 검증
        if (request.getBankTranId() == null || request.getBankTranId().isBlank()) {
            return InquiryVerifyResponse.fail("A0001", "필수 파라미터가 누락되었습니다: bankTranId");
        }
        if (request.getVerifyCode() == null || request.getVerifyCode().isBlank()) {
            return InquiryVerifyResponse.fail("A0001", "필수 파라미터가 누락되었습니다: verifyCode");
        }

        // 인증 세션 조회
        AccountVerification verification = verificationMapper.findByBankTranId(request.getBankTranId());

        if (verification == null) {
            return InquiryVerifyResponse.fail("A0004", "인증 세션을 찾을 수 없습니다");
        }

        // 만료 확인
        if (LocalDateTime.now().isAfter(verification.getExpiredAt())) {
            verificationMapper.updateStatus(verification.getVerificationId(), "EXPIRED");
            return InquiryVerifyResponse.fail("A0004", "인증 세션이 만료되었습니다");
        }

        // 이미 처리된 세션 확인
        if (!"PENDING".equals(verification.getStatus())) {
            return InquiryVerifyResponse.fail("A0004", "이미 처리된 인증 세션입니다");
        }

        // 시도 횟수 증가
        int newAttemptCount = verification.getAttemptCount() + 1;
        verificationMapper.incrementAttemptCount(verification.getVerificationId());

        // 인증코드 비교
        if (!verification.getVerifyCode().equals(request.getVerifyCode())) {
            // 3회 초과 시 실패 처리
            if (newAttemptCount >= MAX_VERIFY_ATTEMPTS) {
                verificationMapper.updateStatus(verification.getVerificationId(), "FAILED");
                return InquiryVerifyResponse.fail("A0005", "인증 시도 횟수를 초과했습니다");
            }
            return InquiryVerifyResponse.fail("A0003", "인증코드가 일치하지 않습니다");
        }

        // 인증 성공 - 핀테크이용번호 생성
        String fintechUseNum = generateFintechUseNum();
        verificationMapper.updateStatus(verification.getVerificationId(), "VERIFIED");

        log.info("[Mock] 인증 성공 - 거래ID: {}, 핀테크번호: {}", request.getBankTranId(), fintechUseNum);

        return InquiryVerifyResponse.success(fintechUseNum);
    }

    /**
     * 입금이체 처리
     */
    @Override
    @Transactional
    public TransferDepositResponse transferDeposit(TransferDepositRequest request) {
        // 필수 파라미터 검증
        if (request.getFintechUseNum() == null || request.getFintechUseNum().isBlank()) {
            return TransferDepositResponse.error("A0001", "필수 파라미터가 누락되었습니다: fintechUseNum");
        }
        if (request.getTranAmt() == null || request.getTranAmt() <= 0) {
            return TransferDepositResponse.error("A0002", "이체금액이 올바르지 않습니다");
        }

        // 거래고유번호 생성
        String bankTranId = generateBankTranId();

        log.info("[Mock] 입금이체 처리 완료 - 거래ID: {}, 금액: {}", bankTranId, request.getTranAmt());

        return TransferDepositResponse.success(bankTranId, request.getTranAmt());
    }

    /**
     * 입금이체 처리 (정산 ID 포함)
     */
    @Transactional
    public TransferDepositResponse transferDepositWithSettlement(Integer settlementId, TransferDepositRequest request) {
        // 기본 검증
        TransferDepositResponse basicResponse = transferDeposit(request);
        if (!"A0000".equals(basicResponse.getRspCode())) {
            // 실패 거래 기록
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

        // 성공 거래 기록
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

    // 4자리 인증코드 생성
    public String generateVerifyCode() {
        Random random = new Random();
        int code = 1000 + random.nextInt(9000); // 1000-9999
        return String.valueOf(code);
    }

    // 거래고유번호 생성
    private String generateBankTranId() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.format("%04d", new Random().nextInt(10000));
        return "MOCK" + timestamp + random;
    }

    // 핀테크이용번호 생성 (24자리)
    private String generateFintechUseNum() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 24);
    }
}
