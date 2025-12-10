package com.moa.web.mock;

import com.moa.dto.openbanking.*;
import com.moa.service.openbanking.MockOpenBankingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Mock 오픈뱅킹 API Controller
 * 금융결제원 오픈뱅킹 API 스펙을 따르는 Mock 서버
 */
@Slf4j
@RestController
@RequestMapping("/mock/openbanking")
@RequiredArgsConstructor
public class MockOpenBankingController {

    private final MockOpenBankingService mockOpenBankingService;

    /**
     * 1원 인증 요청 (수취조회)
     * POST /mock/openbanking/inquiry/receive
     */
    @PostMapping("/inquiry/receive")
    public ResponseEntity<InquiryReceiveResponse> inquiryReceive(
            @Valid @RequestBody InquiryReceiveRequest request) {

        log.info("[Mock 오픈뱅킹] 1원 인증 요청 - 은행: {}, 계좌: {}",
                request.getBankCodeStd(), maskAccountNum(request.getAccountNum()));

        InquiryReceiveResponse response = mockOpenBankingService.requestVerification(request);

        log.info("[Mock 오픈뱅킹] 1원 인증 응답 - 코드: {}, 거래ID: {}",
                response.getRspCode(), response.getBankTranId());

        return ResponseEntity.ok(response);
    }

    /**
     * 인증코드 검증
     * POST /mock/openbanking/inquiry/verify
     */
    @PostMapping("/inquiry/verify")
    public ResponseEntity<InquiryVerifyResponse> verifyCode(
            @Valid @RequestBody InquiryVerifyRequest request) {

        log.info("[Mock 오픈뱅킹] 인증코드 검증 요청 - 거래ID: {}", request.getBankTranId());

        InquiryVerifyResponse response = mockOpenBankingService.verifyCode(request);

        log.info("[Mock 오픈뱅킹] 인증코드 검증 응답 - 코드: {}, 인증결과: {}",
                response.getRspCode(), response.isVerified());

        return ResponseEntity.ok(response);
    }

    /**
     * 입금이체
     * POST /mock/openbanking/transfer/deposit
     */
    @PostMapping("/transfer/deposit")
    public ResponseEntity<TransferDepositResponse> transferDeposit(
            @Valid @RequestBody TransferDepositRequest request) {

        log.info("[Mock 오픈뱅킹] 입금이체 요청 - 핀테크번호: {}, 금액: {}",
                maskFintechNum(request.getFintechUseNum()), request.getTranAmt());

        TransferDepositResponse response = mockOpenBankingService.transferDeposit(request);

        log.info("[Mock 오픈뱅킹] 입금이체 응답 - 코드: {}, 거래ID: {}",
                response.getRspCode(), response.getBankTranId());

        return ResponseEntity.ok(response);
    }

    // 계좌번호 마스킹 (앞 4자리 + **** + 뒤 4자리)
    private String maskAccountNum(String accountNum) {
        if (accountNum == null || accountNum.length() < 8) {
            return "****";
        }
        return accountNum.substring(0, 4) + "****" +
                accountNum.substring(accountNum.length() - 4);
    }

    // 핀테크이용번호 마스킹
    private String maskFintechNum(String fintechNum) {
        if (fintechNum == null || fintechNum.length() < 8) {
            return "****";
        }
        return fintechNum.substring(0, 4) + "****";
    }
}
