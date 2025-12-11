package com.moa.service.payment;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moa.common.exception.BusinessException;
import com.moa.common.exception.ErrorCode;
import com.moa.common.exception.TossPaymentException;
import com.moa.config.TossPaymentConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TossPaymentService {

    private final TossPaymentConfig tossPaymentConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public void confirmPayment(String paymentKey, String orderId, Integer amount) {
        String url = "https://api.tosspayments.com/v1/payments/confirm";

        // 1. 헤더 설정 (Basic Auth)
        HttpHeaders headers = createHeaders();

        // 2. 바디 설정
        Map<String, Object> body = Map.of(
                "paymentKey", paymentKey,
                "orderId", orderId,
                "amount", amount);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            // 3. API 호출
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new BusinessException(ErrorCode.PAYMENT_FAILED);
            }

            // 응답 로깅 (디버깅용)
            log.info("Toss Payment Confirm Response: {}", response.getBody());

        } catch (HttpClientErrorException e) {
            handleTossError(e);
        } catch (Exception e) {
            log.error("Toss Payment Confirm Error", e);
            throw new BusinessException(ErrorCode.PAYMENT_FAILED);
        }
    }

    /**
     * 결제 취소
     * 
     * @param paymentKey   결제 키
     * @param cancelReason 취소 사유
     * @param cancelAmount 취소 금액 (전액 취소 시 null 가능하지만, 명시적으로 넣는 것 권장)
     */
    public void cancelPayment(String paymentKey, String cancelReason, Integer cancelAmount) {
        String url = "https://api.tosspayments.com/v1/payments/" + paymentKey + "/cancel";

        // 1. 헤더 설정 (Basic Auth)
        HttpHeaders headers = createHeaders();

        // 2. 바디 설정
        Map<String, Object> body = Map.of(
                "cancelReason", cancelReason,
                "cancelAmount", cancelAmount);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            // 3. API 호출
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new BusinessException(ErrorCode.PAYMENT_FAILED);
            }

            log.info("Toss Payment Cancel Response: {}", response.getBody());

        } catch (HttpClientErrorException e) {
            handleTossError(e);
        } catch (Exception e) {
            log.error("Toss Payment Cancel Error", e);
            throw new BusinessException(ErrorCode.PAYMENT_FAILED);
        }
    }

    /**
     * 빌링키 발급 (카드 등록)
     *
     * @param authKey     인증 키
     * @param customerKey 고객 키 (userId)
     * @return 빌링키 발급 응답 (billingKey, card 정보 포함)
     */
    public Map<String, Object> issueBillingKey(String authKey, String customerKey) {
        String url = "https://api.tosspayments.com/v1/billing/authorizations/issue";

        // 1. 헤더 설정 (Basic Auth)
        HttpHeaders headers = createHeaders();

        // 2. 바디 설정
        Map<String, Object> body = Map.of(
                "authKey", authKey,
                "customerKey", customerKey);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            // 3. API 호출
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    org.springframework.http.HttpMethod.POST,
                    request,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {
                    });

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new BusinessException(ErrorCode.PAYMENT_FAILED);
            }

            Map<String, Object> resBody = response.getBody();
            if (resBody != null && resBody.containsKey("billingKey")) {
                log.info("Toss Billing Key Issued: {}", resBody.get("billingKey"));
                return resBody;
            } else {
                throw new BusinessException(ErrorCode.PAYMENT_FAILED);
            }

        } catch (HttpClientErrorException e) {
            handleTossError(e);
            throw e; // Unreachable, but for compiler
        } catch (Exception e) {
            log.error("Toss Billing Key Issue Error", e);
            throw new BusinessException(ErrorCode.PAYMENT_FAILED);
        }
    }

    /**
     * 빌링키 결제 (자동 결제)
     *
     * @param billingKey  빌링키
     * @param orderId     주문 ID
     * @param amount      결제 금액
     * @param orderName   주문명
     * @param customerKey 고객 키 (userId)
     * @return 결제 키 (paymentKey)
     */
    public String payWithBillingKey(String billingKey, String orderId, Integer amount, String orderName,
            String customerKey) {
        String url = "https://api.tosspayments.com/v1/billing/" + billingKey;

        // 1. 헤더 설정 (Basic Auth)
        HttpHeaders headers = createHeaders();

        // 2. 바디 설정
        Map<String, Object> body = Map.of(
                "amount", amount,
                "orderId", orderId,
                "orderName", orderName,
                "customerKey", customerKey);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    org.springframework.http.HttpMethod.POST,
                    request,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {
                    });

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new BusinessException(ErrorCode.PAYMENT_FAILED);
            }

            Map<String, Object> resBody = response.getBody();
            if (resBody != null && resBody.containsKey("paymentKey")) {
                return (String) resBody.get("paymentKey");
            } else {
                throw new BusinessException(ErrorCode.PAYMENT_FAILED);
            }

        } catch (HttpClientErrorException e) {
            handleTossError(e);
            return null; // Unreachable
        } catch (Exception e) {
            log.error("Toss Billing Payment Error", e);
            throw new BusinessException(ErrorCode.PAYMENT_FAILED);
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String secretKey = tossPaymentConfig.getSecretApiKey() + ":";
        String encodedAuth = Base64.getEncoder().encodeToString(secretKey.getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + encodedAuth);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }

    private void handleTossError(HttpClientErrorException e) {
        log.error("Toss Payment API Error: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());

        try {
            JsonNode root = objectMapper.readTree(e.getResponseBodyAsString());
            String code = root.path("code").asText();
            String message = root.path("message").asText();

            throw new TossPaymentException(ErrorCode.PAYMENT_FAILED, code, message);
        } catch (TossPaymentException tpe) {
            throw tpe;
        } catch (Exception parseException) {
            log.error("Failed to parse Toss error response", parseException);
            throw new BusinessException(ErrorCode.PAYMENT_FAILED, "결제 처리 중 오류가 발생했습니다. (응답 파싱 실패)");
        }
    }
}
