package com.moa.service.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moa.common.exception.TossPaymentException;
import com.moa.config.TossPaymentConfig;

class TossPaymentErrorTest {

    private TossPaymentService tossPaymentService;
    private RestTemplate restTemplate;
    private TossPaymentConfig tossPaymentConfig;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        tossPaymentConfig = mock(TossPaymentConfig.class);
        objectMapper = new ObjectMapper();

        when(tossPaymentConfig.getSecretApiKey()).thenReturn("test_sk_key");

        tossPaymentService = new TossPaymentService(tossPaymentConfig, restTemplate, objectMapper);
    }

    @Test
    @DisplayName("Toss API 404 에러 시 TossPaymentException 발생 및 에러 코드 파싱 확인")
    void testConfirmPayment_404Error() {
        // Given
        String errorResponseBody = "{" +
                "\"code\": \"NOT_FOUND_PAYMENT\"," +
                "\"message\": \"존재하지 않는 결제 정보 입니다.\"" +
                "}";

        HttpClientErrorException exception = HttpClientErrorException.create(
                HttpStatus.NOT_FOUND,
                "Not Found",
                HttpHeaders.EMPTY,
                errorResponseBody.getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);

        when(restTemplate.postForEntity(any(), any(), eq(String.class)))
                .thenThrow(exception);

        // When & Then
        TossPaymentException thrown = assertThrows(TossPaymentException.class, () -> {
            tossPaymentService.confirmPayment("test_payment_key", "test_order_id", 10000);
        });

        // Verify
        assertEquals("NOT_FOUND_PAYMENT", thrown.getTossErrorCode());
        assertEquals("존재하지 않는 결제 정보 입니다.", thrown.getMessage());
    }

    @Test
    @DisplayName("Toss API 결제 취소 에러 시 TossPaymentException 발생 확인")
    void testCancelPayment_Error() {
        // Given
        String errorResponseBody = "{" +
                "\"code\": \"ALREADY_CANCELED_PAYMENT\"," +
                "\"message\": \"이미 취소된 결제 입니다.\"" +
                "}";

        HttpClientErrorException exception = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                HttpHeaders.EMPTY,
                errorResponseBody.getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);

        when(restTemplate.postForEntity(any(), any(), eq(String.class)))
                .thenThrow(exception);

        // When & Then
        TossPaymentException thrown = assertThrows(TossPaymentException.class, () -> {
            tossPaymentService.cancelPayment("test_payment_key", "Just Test", null);
        });

        // Verify
        assertEquals("ALREADY_CANCELED_PAYMENT", thrown.getTossErrorCode());
        assertEquals("이미 취소된 결제 입니다.", thrown.getMessage());
    }
}
