package com.moa.web.party;

import com.moa.dao.party.PartyDao;
import com.moa.dao.push.PushDao;
import com.moa.domain.Push;
import com.moa.domain.enums.PartyStatus;
import com.moa.dto.party.request.PartyCreateRequest;
import com.moa.dto.payment.request.PaymentRequest;
import com.moa.dto.party.response.PartyDetailResponse;
import com.moa.service.party.PartyService;
import com.moa.service.payment.TossPaymentService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("파티 서비스 Push 알림 통합 테스트")
class PartyServicePushTest {
    
    @Autowired
    private PartyService partyService;
    
    @Autowired
    private PartyDao partyDao;
    
    @Autowired
    private PushDao pushDao;
    
    @MockBean
    private TossPaymentService tossPaymentService;
    
    private Integer testPartyId;
    
    private static final String LEADER_ID = "user001@gmail.com";
    private static final String MEMBER1_ID = "user002@naver.com";
    private static final String MEMBER2_ID = "user003@daum.net";
    private static final String MEMBER3_ID = "user004@gmail.com";
    
    @BeforeEach
    void setupMock() {
        doNothing().when(tossPaymentService).confirmPayment(anyString(), anyString(), anyInt());
    }
    
    @Test
    @Order(1)
    @DisplayName("1. 파티 생성")
    void test1_createParty_NoAlarm() {
        PartyCreateRequest request = PartyCreateRequest.builder()
                .productId(1)
                .maxMembers(4)
                .startDate(LocalDate.now().plusDays(7))
                .endDate(LocalDate.now().plusMonths(1))
                .accountId(1)
                .ottId("test@netflix.com")
                .ottPassword("test1234")
                .build();
        
        int pushCountBefore = pushDao.getMyPushList(LEADER_ID, 0, 100).size();
        
        PartyDetailResponse party = partyService.createParty(LEADER_ID, request);
        testPartyId = party.getPartyId();
        
        partyDao.updatePartyStatus(testPartyId, PartyStatus.RECRUITING);
        
        int pushCountAfter = pushDao.getMyPushList(LEADER_ID, 0, 100).size();
        
        assertEquals(pushCountBefore, pushCountAfter);
        
        System.out.println("✅ Test1: Party Created (ID=" + testPartyId + ")");
    }
    
    @Test
    @Order(2)
    @DisplayName("2. 첫 번째 멤버 가입")
    void test2_firstMemberJoin_PartyJoinAlarm() {
        PaymentRequest paymentRequest = PaymentRequest.builder()
                .amount(4250)
                .paymentMethod("CARD")
                .tossPaymentKey("TEST_001")
                .orderId("ORDER_001")
                .build();
        
        int pushCountBefore = pushDao.getMyPushList(MEMBER1_ID, 0, 100).size();
        
        partyService.joinParty(testPartyId, MEMBER1_ID, paymentRequest);
        
        int pushCountAfter = pushDao.getMyPushList(MEMBER1_ID, 0, 100).size();
        
        assertEquals(pushCountBefore + 1, pushCountAfter);
        
        Push joinPush = pushDao.getMyPushList(MEMBER1_ID, 0, 100).stream()
                .filter(p -> "PARTY_JOIN".equals(p.getPushCode()))
                .findFirst()
                .orElse(null);
        
        assertNotNull(joinPush);
        assertTrue(joinPush.getTitle().contains("파티 가입"));
        
        System.out.println("✅ Test2: Member1 Joined + PARTY_JOIN Push");
    }
    
    @Test
    @Order(3)
    @DisplayName("3. 두 번째 멤버 가입")
    void test3_secondMemberJoin_PartyJoinAlarm() {
        PaymentRequest paymentRequest = PaymentRequest.builder()
                .amount(4250)
                .paymentMethod("CARD")
                .tossPaymentKey("TEST_002")
                .orderId("ORDER_002")
                .build();
        
        int pushCountBefore = pushDao.getMyPushList(MEMBER2_ID, 0, 100).size();
        
        partyService.joinParty(testPartyId, MEMBER2_ID, paymentRequest);
        
        int pushCountAfter = pushDao.getMyPushList(MEMBER2_ID, 0, 100).size();
        
        assertEquals(pushCountBefore + 1, pushCountAfter);
        
        System.out.println("✅ Test3: Member2 Joined + PARTY_JOIN Push");
    }
}