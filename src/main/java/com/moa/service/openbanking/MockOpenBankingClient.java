package com.moa.service.openbanking;

import com.moa.dto.openbanking.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
@Profile("!prod")
@RequiredArgsConstructor
public class MockOpenBankingClient implements OpenBankingClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${openbanking.mock.base-url:http://localhost:8080}")
    private String mockBaseUrl;

    @Override
    public InquiryReceiveResponse requestVerification(InquiryReceiveRequest request) {
        log.info("[MockClient] 1원 인증 요청 - 은행: {}, 계좌: {}",
                request.getBankCodeStd(), maskAccountNum(request.getAccountNum()));

        try {
            WebClient webClient = createInsecureWebClient();

            InquiryReceiveResponse response = webClient.post()
                    .uri("/mock/openbanking/inquiry/receive")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(InquiryReceiveResponse.class)
                    .block();

            log.info("[MockClient] 1원 인증 응답 - 코드: {}", response.getRspCode());
            return response;

        } catch (Exception e) {
            log.error("[MockClient] 1원 인증 요청 실패", e);
            return InquiryReceiveResponse.error("A0007", "Mock 서버 연결 실패: " + e.getMessage());
        }
    }

    @Override
    public InquiryVerifyResponse verifyCode(InquiryVerifyRequest request) {
        log.info("[MockClient] 인증코드 검증 요청 - 거래ID: {}", request.getBankTranId());

        try {
            WebClient webClient = createInsecureWebClient();

            InquiryVerifyResponse response = webClient.post()
                    .uri("/mock/openbanking/inquiry/verify")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(InquiryVerifyResponse.class)
                    .block();

            log.info("[MockClient] 인증코드 검증 응답 - 코드: {}, 결과: {}",
                    response.getRspCode(), response.isVerified());
            return response;

        } catch (Exception e) {
            log.error("[MockClient] 인증코드 검증 요청 실패", e);
            return InquiryVerifyResponse.fail("A0007", "Mock 서버 연결 실패: " + e.getMessage());
        }
    }

    @Override
    public TransferDepositResponse transferDeposit(TransferDepositRequest request) {
        log.info("[MockClient] 입금이체 요청 - 금액: {}", request.getTranAmt());

        try {
            WebClient webClient = createInsecureWebClient();

            TransferDepositResponse response = webClient.post()
                    .uri("/mock/openbanking/transfer/deposit")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(TransferDepositResponse.class)
                    .block();

            log.info("[MockClient] 입금이체 응답 - 코드: {}, 거래ID: {}",
                    response.getRspCode(), response.getBankTranId());
            return response;

        } catch (Exception e) {
            log.error("[MockClient] 입금이체 요청 실패", e);
            return TransferDepositResponse.error("A0007", "Mock 서버 연결 실패: " + e.getMessage());
        }
    }

    private WebClient createInsecureWebClient() {
        try {
            var sslContext = io.netty.handler.ssl.SslContextBuilder.forClient()
                    .trustManager(io.netty.handler.ssl.util.InsecureTrustManagerFactory.INSTANCE)
                    .build();

            var httpClient = reactor.netty.http.client.HttpClient.create()
                    .secure(t -> t.sslContext(sslContext));

            return webClientBuilder.baseUrl(mockBaseUrl)
                    .clientConnector(
                            new org.springframework.http.client.reactive.ReactorClientHttpConnector(httpClient))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String maskAccountNum(String accountNum) {
        if (accountNum == null || accountNum.length() < 8) {
            return "****";
        }
        return accountNum.substring(0, 4) + "****" +
                accountNum.substring(accountNum.length() - 4);
    }
}
