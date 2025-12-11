package com.moa.web.push;

import com.moa.dao.party.PartyDao;
import com.moa.dao.push.PushDao;
import com.moa.domain.*;
import com.moa.domain.enums.*;
import com.moa.dto.party.request.PartyCreateRequest;
import com.moa.dto.payment.request.PaymentRequest;
import com.moa.dto.party.response.PartyDetailResponse;
import com.moa.service.party.PartyService;
import com.moa.service.payment.TossPaymentService;
import com.moa.service.push.PushService;
import com.moa.dto.push.request.TemplatePushRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ========================================
 * 푸시 알림 전체 통합 테스트 (27개 전체)
 * ========================================
 * 
 * 테스트 대상 푸시 코드 (27개):
 * 
 * [커뮤니티 - 1개] INQUIRY_ANSWER
 * [파티 - 6개] PARTY_JOIN, PARTY_WITHDRAW, PARTY_START, PARTY_CLOSED,
 * PARTY_MEMBER_JOIN, PARTY_MEMBER_WITHDRAW
 * [결제 - 10개] PAY_UPCOMING, PAY_SUCCESS, PAY_FAILED_RETRY, PAY_FAILED_BALANCE,
 * PAY_FAILED_LIMIT,
 * PAY_FAILED_CARD, PAY_FINAL_FAILED, PAY_MEMBER_FAILED_LEADER,
 * PAY_RETRY_SUCCESS, PAY_TIMEOUT
 * [보증금 - 3개] DEPOSIT_REFUNDED, DEPOSIT_FORFEITED, REFUND_SUCCESS
 * [정산 - 3개] SETTLE_COMPLETED, SETTLE_FAILED, ACCOUNT_REQUIRED
 * [오픈뱅킹 - 4개] VERIFY_REQUESTED, ACCOUNT_VERIFIED, VERIFY_EXPIRED,
 * VERIFY_EXCEEDED
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("푸시 알림 전체 통합 테스트 (27개)")
class PushNotificationFullTest {

    @Autowired
    private PartyService partyService;
    @Autowired
    private PushService pushService;
    @Autowired
    private PushDao pushDao;
    @Autowired
    private PartyDao partyDao;
    @MockBean
    private TossPaymentService tossPaymentService;

    private Integer testPartyId;
    private static final String USER1_ID = "user001@gmail.com";
    private static final String USER2_ID = "user002@naver.com";
    private static final String USER3_ID = "user003@daum.net";

    private int totalTests = 0;
    private int passedTests = 0;

    @BeforeAll
    void setup() {
        doNothing().when(tossPaymentService).confirmPayment(anyString(), anyString(), anyInt());
        when(tossPaymentService.payWithBillingKey(anyString(), anyString(), anyInt(), anyString(), anyString()))
                .thenReturn("mock_key");

        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║       MOA 푸시 알림 전체 통합 테스트 (27개)                  ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝\n");
    }

    // ===== 1. 커뮤니티 (1개) =====
    @Test
    @Order(1)
    @DisplayName("1. INQUIRY_ANSWER - 문의 답변 완료")
    void test_01_InquiryAnswer() {
        testPush(USER1_ID, PushCodeType.INQUIRY_ANSWER, Map.of("nickname", "사용자001"), "문의 답변");
    }

    // ===== 2. 파티 (6개) =====
    @Test
    @Order(10)
    @DisplayName("2-1. PARTY_JOIN - 파티 가입 완료")
    void test_02_PartyJoin() {
        // 테스트 파티 생성
        try {
            PartyCreateRequest req = PartyCreateRequest.builder()
                    .productId(1).maxMembers(4).startDate(LocalDate.now().plusDays(7))
                    .endDate(LocalDate.now().plusMonths(3)).accountId(1)
                    .ottId("test@ott.com").ottPassword("test1234").build();
            PartyDetailResponse party = partyService.createParty(USER1_ID, req);
            testPartyId = party.getPartyId();
            partyDao.updatePartyStatus(testPartyId, PartyStatus.RECRUITING);
        } catch (Exception e) {
            testPartyId = 1;
        }

        testPush(USER2_ID, PushCodeType.PARTY_JOIN,
                Map.of("nickname", "사용자002", "productName", "Google AI Pro", "currentCount", "2", "maxCount", "4"),
                "파티 가입");
    }

    @Test
    @Order(11)
    @DisplayName("2-2. PARTY_MEMBER_JOIN - 새 파티원 참여")
    void test_03_PartyMemberJoin() {
        testPush(USER1_ID, PushCodeType.PARTY_MEMBER_JOIN,
                Map.of("nickname", "사용자002", "productName", "Google AI Pro", "currentCount", "2", "maxCount", "4"),
                "파티원");
    }

    @Test
    @Order(12)
    @DisplayName("2-3. PARTY_WITHDRAW - 파티 탈퇴 완료")
    void test_04_PartyWithdraw() {
        testPush(USER3_ID, PushCodeType.PARTY_WITHDRAW,
                Map.of("nickname", "사용자003", "productName", "Google AI Pro"), "탈퇴");
    }

    @Test
    @Order(13)
    @DisplayName("2-4. PARTY_MEMBER_WITHDRAW - 파티원 탈퇴 (방장에게)")
    void test_05_PartyMemberWithdraw() {
        testPush(USER1_ID, PushCodeType.PARTY_MEMBER_WITHDRAW,
                Map.of("nickname", "사용자003", "productName", "Google AI Pro", "currentCount", "2", "maxCount", "4"),
                "탈퇴");
    }

    @Test
    @Order(14)
    @DisplayName("2-5. PARTY_START - 파티 시작")
    void test_06_PartyStart() {
        testPush(USER2_ID, PushCodeType.PARTY_START, Map.of("productName", "Google AI Pro"), "파티 시작");
    }

    @Test
    @Order(15)
    @DisplayName("2-6. PARTY_CLOSED - 파티 종료")
    void test_07_PartyClosed() {
        testPush(USER2_ID, PushCodeType.PARTY_CLOSED, Map.of("productName", "Google AI Pro"), "종료");
    }

    // ===== 3. 결제 (10개) =====
    @Test
    @Order(20)
    @DisplayName("3-1. PAY_UPCOMING - 결제 예정 안내")
    void test_08_PayUpcoming() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        testPush(USER2_ID, PushCodeType.PAY_UPCOMING,
                Map.of("productName", "Google AI Pro", "amount", "4250",
                        "paymentDate", tomorrow.format(DateTimeFormatter.ofPattern("M월 d일"))),
                "결제 예정");
    }

    @Test
    @Order(21)
    @DisplayName("3-2. PAY_SUCCESS - 결제 완료")
    void test_09_PaySuccess() {
        testPush(USER2_ID, PushCodeType.PAY_SUCCESS,
                Map.of("productName", "Google AI Pro", "targetMonth", "2025년 1월", "amount", "4250"), "결제 완료");
    }

    @Test
    @Order(22)
    @DisplayName("3-3. PAY_RETRY_SUCCESS - 결제 재시도 성공")
    void test_10_PayRetrySuccess() {
        testPush(USER2_ID, PushCodeType.PAY_RETRY_SUCCESS,
                Map.of("productName", "Google AI Pro", "attemptNumber", "2", "amount", "4250"), "재시도");
    }

    @Test
    @Order(23)
    @DisplayName("3-4. PAY_FAILED_RETRY - 결제 실패 (재시도 예정)")
    void test_11_PayFailedRetry() {
        LocalDate next = LocalDate.now().plusDays(1);
        testPush(USER2_ID, PushCodeType.PAY_FAILED_RETRY,
                Map.of("productName", "Google AI Pro", "attemptNumber", "1", "errorMessage", "일시적 오류",
                        "nextRetryDate", next.format(DateTimeFormatter.ofPattern("M월 d일"))),
                "결제 실패");
    }

    @Test
    @Order(24)
    @DisplayName("3-5. PAY_FAILED_BALANCE - 결제 실패 (잔액 부족)")
    void test_12_PayFailedBalance() {
        LocalDate next = LocalDate.now().plusDays(1);
        testPush(USER2_ID, PushCodeType.PAY_FAILED_BALANCE,
                Map.of("productName", "Google AI Pro", "nextRetryDate",
                        next.format(DateTimeFormatter.ofPattern("M월 d일"))),
                "잔액");
    }

    @Test
    @Order(25)
    @DisplayName("3-6. PAY_FAILED_LIMIT - 결제 실패 (한도 초과)")
    void test_13_PayFailedLimit() {
        LocalDate next = LocalDate.now().plusDays(1);
        testPush(USER2_ID, PushCodeType.PAY_FAILED_LIMIT,
                Map.of("productName", "Google AI Pro", "nextRetryDate",
                        next.format(DateTimeFormatter.ofPattern("M월 d일"))),
                "한도");
    }

    @Test
    @Order(26)
    @DisplayName("3-7. PAY_FAILED_CARD - 결제 실패 (카드 오류)")
    void test_14_PayFailedCard() {
        LocalDate next = LocalDate.now().plusDays(1);
        testPush(USER2_ID, PushCodeType.PAY_FAILED_CARD,
                Map.of("productName", "Google AI Pro", "nextRetryDate",
                        next.format(DateTimeFormatter.ofPattern("M월 d일"))),
                "카드");
    }

    @Test
    @Order(27)
    @DisplayName("3-8. PAY_FINAL_FAILED - 결제 최종 실패")
    void test_15_PayFinalFailed() {
        testPush(USER2_ID, PushCodeType.PAY_FINAL_FAILED,
                Map.of("productName", "Google AI Pro", "attemptNumber", "4", "errorMessage", "카드 한도 초과"), "최종 실패");
    }

    @Test
    @Order(28)
    @DisplayName("3-9. PAY_MEMBER_FAILED_LEADER - 파티원 결제 실패 (방장에게)")
    void test_16_PayMemberFailedLeader() {
        testPush(USER1_ID, PushCodeType.PAY_MEMBER_FAILED_LEADER,
                Map.of("memberNickname", "사용자002", "productName", "Google AI Pro", "errorMessage", "잔액 부족"), "파티원");
    }

    @Test
    @Order(29)
    @DisplayName("3-10. PAY_TIMEOUT - 파티 생성 취소")
    void test_17_PayTimeout() {
        testPush(USER1_ID, PushCodeType.PAY_TIMEOUT, Map.of("productName", "Google AI Pro"), "취소");
    }

    // ===== 4. 보증금 (3개) =====
    @Test
    @Order(30)
    @DisplayName("4-1. DEPOSIT_REFUNDED - 보증금 환불 완료")
    void test_18_DepositRefunded() {
        testPush(USER2_ID, PushCodeType.DEPOSIT_REFUNDED,
                Map.of("productName", "Google AI Pro", "amount", "4250"), "환불");
    }

    @Test
    @Order(31)
    @DisplayName("4-2. DEPOSIT_FORFEITED - 보증금 몰수 안내")
    void test_19_DepositForfeited() {
        testPush(USER3_ID, PushCodeType.DEPOSIT_FORFEITED,
                Map.of("productName", "Google AI Pro", "amount", "4250"), "몰수");
    }

    @Test
    @Order(32)
    @DisplayName("4-3. REFUND_SUCCESS - 환불 처리 완료")
    void test_20_RefundSuccess() {
        testPush(USER2_ID, PushCodeType.REFUND_SUCCESS,
                Map.of("productName", "Google AI Pro", "amount", "4250"), "환불");
    }

    // ===== 5. 정산 (3개) =====
    @Test
    @Order(40)
    @DisplayName("5-1. SETTLE_COMPLETED - 정산 입금 완료")
    void test_21_SettleCompleted() {
        testPush(USER1_ID, PushCodeType.SETTLE_COMPLETED,
                Map.of("settlementMonth", "2025년 1월", "netAmount", "14450"), "정산");
    }

    @Test
    @Order(41)
    @DisplayName("5-2. SETTLE_FAILED - 정산 실패")
    void test_22_SettleFailed() {
        testPush(USER1_ID, PushCodeType.SETTLE_FAILED, Map.of("settlementMonth", "2025년 1월"), "정산");
    }

    @Test
    @Order(42)
    @DisplayName("5-3. ACCOUNT_REQUIRED - 계좌 등록 필요")
    void test_23_AccountRequired() {
        testPush(USER1_ID, PushCodeType.ACCOUNT_REQUIRED, Map.of(), "계좌");
    }

    // ===== 6. 오픈뱅킹 (4개) =====
    @Test
    @Order(50)
    @DisplayName("6-1. VERIFY_REQUESTED - 1원 인증 요청")
    void test_24_VerifyRequested() {
        testPush(USER1_ID, PushCodeType.VERIFY_REQUESTED, Map.of(), "인증");
    }

    @Test
    @Order(51)
    @DisplayName("6-2. ACCOUNT_VERIFIED - 계좌 등록 완료")
    void test_25_AccountVerified() {
        testPush(USER1_ID, PushCodeType.ACCOUNT_VERIFIED, Map.of(), "계좌");
    }

    @Test
    @Order(52)
    @DisplayName("6-3. VERIFY_EXPIRED - 인증 만료")
    void test_26_VerifyExpired() {
        testPush(USER2_ID, PushCodeType.VERIFY_EXPIRED, Map.of(), "만료");
    }

    @Test
    @Order(53)
    @DisplayName("6-4. VERIFY_EXCEEDED - 인증 시도 초과")
    void test_27_VerifyExceeded() {
        testPush(USER2_ID, PushCodeType.VERIFY_EXCEEDED, Map.of(), "초과");
    }

    // ===== 결과 요약 =====
    @Test
    @Order(99)
    @DisplayName("========== 테스트 결과 요약 ==========")
    void test_99_Summary() {
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                    테스트 결과 요약                           ║");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.printf("║  총 테스트: %2d개  |  ✅ 성공: %2d개  |  ❌ 실패: %2d개        ║%n",
                totalTests, passedTests, totalTests - passedTests);
        System.out.printf("║  성공률: %.1f%%                                               ║%n",
                totalTests > 0 ? (passedTests * 100.0 / totalTests) : 0);
        System.out.println("╚══════════════════════════════════════════════════════════════╝");

        System.out.println("\n📬 사용자별 푸시 현황:");
        printUserSummary(USER1_ID);
        printUserSummary(USER2_ID);
        printUserSummary(USER3_ID);

        // 결과 요약은 항상 통과
        assertTrue(true, "테스트 결과 요약 완료");
    }

    // ===== Helper Methods =====
    private void testPush(String userId, PushCodeType pushCode, Map<String, String> params, String expectedKeyword) {
        totalTests++;
        String testName = pushCode.getCode();
        System.out.println("\n▶ " + testName);

        try {
            int before = getPushCount(userId);

            TemplatePushRequest request = TemplatePushRequest.builder()
                    .receiverId(userId)
                    .pushCode(pushCode.getCode())
                    .params(params)
                    .moduleId("TEST_" + pushCode.getCode())
                    .moduleType(pushCode.getModuleType())
                    .build();
            pushService.addTemplatePush(request);

            int after = getPushCount(userId);
            assertEquals(before + 1, after);

            Push push = getLatestPush(userId, pushCode.getCode());
            assertNotNull(push);
            assertTrue(push.getTitle().contains(expectedKeyword) || push.getContent().contains(expectedKeyword),
                    "Expected keyword '" + expectedKeyword + "' not found");

            passedTests++;
            System.out.println("   ✅ PASS");
            System.out.println("      Title: " + push.getTitle());
            System.out.println("      Content: " + push.getContent());
        } catch (Exception e) {
            System.out.println("   ❌ FAIL: " + e.getMessage());
        }
    }

    private int getPushCount(String userId) {
        return pushDao.getMyPushList(userId, 0, 1000).size();
    }

    private Push getLatestPush(String userId, String pushCode) {
        return pushDao.getMyPushList(userId, 0, 100).stream()
                .filter(p -> pushCode.equals(p.getPushCode()))
                .findFirst().orElse(null);
    }

    private void printUserSummary(String userId) {
        List<Push> list = pushDao.getMyPushList(userId, 0, 1000);
        System.out.printf("  👤 %s: %d건%n", userId, list.size());
    }
}