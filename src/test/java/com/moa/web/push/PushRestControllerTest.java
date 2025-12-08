package com.moa.web.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moa.dao.push.PushDao;
import com.moa.domain.Push;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PushRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PushDao pushDao;

    private static Push testPush;
    private static Map<String, Object> testTemplatePush;

    @BeforeAll
    static void setUpAll() {
        testPush = new Push();
        testPush.setReceiverId("user001@gmail.com");
        testPush.setPushCode("PAYMENT_SUCCESS");
        testPush.setTitle("테스트 알림 제목");
        testPush.setContent("테스트 알림 내용입니다.");
        testPush.setModuleId("TEST_MODULE_001");
        testPush.setModuleType("TEST");

        testTemplatePush = new HashMap<>();
        testTemplatePush.put("receiverId", "user002@gmail.com");
        testTemplatePush.put("codeName", "PAYMENT_SUCCESS");
        Map<String, String> params = new HashMap<>();
        params.put("닉네임", "사용자02");
        params.put("금액", "5000");
        testTemplatePush.put("params", params);
    }

    @Test
    @Order(1)
    @DisplayName("Push DAO 기본 발송 테스트")
    void testAddPushDao() {
        int result = pushDao.addPush(testPush);
        assertEquals(1, result);
    }

    @Test
    @Order(2)
    @DisplayName("Push 단건 조회 테스트")
    void testGetPush() {
        int pushId = 1;
        Push push = pushDao.getPush(pushId);

        assertNotNull(push);
        assertEquals(pushId, push.getPushId());
        assertNotNull(push.getReceiverId());
        assertNotNull(push.getTitle());
    }

    @Test
    @Order(3)
    @DisplayName("Push 목록 조회 테스트")
    void testGetPushList() {
        String receiverId = "user001@gmail.com";
        List<Push> pushList = pushDao.getPushList(receiverId, 0, 10);

        assertNotNull(pushList);
        assertTrue(pushList.size() > 0);
    }

    @Test
    @Order(4)
    @DisplayName("Push 전체 개수 조회 테스트")
    void testGetPushTotalCount() {
        String receiverId = "user001@gmail.com";
        int totalCount = pushDao.getPushTotalCount(receiverId);

        assertTrue(totalCount > 0);
    }

    @Test
    @Order(5)
    @DisplayName("안 읽은 Push 개수 조회 테스트")
    void testGetUnreadCount() {
        String receiverId = "user003@daum.net";
        int unreadCount = pushDao.getUnreadCount(receiverId);

        assertTrue(unreadCount >= 0);
    }

    @Test
    @Order(6)
    @DisplayName("Push 읽음 처리 테스트")
    void testMarkAsRead() {
        int pushId = 3;
        int result = pushDao.markAsRead(pushId);

        assertEquals(1, result);

        Push push = pushDao.getPush(pushId);
        assertEquals("Y", push.getIsRead());
        assertNotNull(push.getReadAt());
    }

    @Test
    @Order(7)
    @DisplayName("Push 삭제 처리 테스트")
    void testMarkAsDeleted() {
        int pushId = 2;
        int result = pushDao.markAsDeleted(pushId);

        assertEquals(1, result);

        Push push = pushDao.getPush(pushId);
        assertEquals("Y", push.getIsDeleted());
    }

    @Test
    @Order(8)
    @DisplayName("PushCode 템플릿 조회 테스트")
    void testGetPushCodeByName() {
        String codeName = "INQUIRY_ANSWER";
        var pushCode = pushDao.getPushCodeByName(codeName);

        assertNotNull(pushCode);
        assertEquals(codeName, pushCode.getCodeName());
        assertNotNull(pushCode.getTitleTemplate());
        assertNotNull(pushCode.getContentTemplate());
    }

    @Test
    @Order(9)
    @DisplayName("문의 답변 완료 Push 발송 시나리오 테스트")
    void testInquiryAnswerPushScenario() {
        var pushCode = pushDao.getPushCodeByName("INQUIRY_ANSWER");
        assertNotNull(pushCode);

        String receiverId = "user001@gmail.com";
        String nickname = "사용자001";

        String title = pushCode.getTitleTemplate().replace("{nickname}", nickname);
        String content = pushCode.getContentTemplate().replace("{nickname}", nickname);

        Push push = new Push();
        push.setReceiverId(receiverId);
        push.setPushCode("INQUIRY_ANSWER");
        push.setTitle(title);
        push.setContent(content);
        push.setModuleId("4");
        push.setModuleType("COMMUNITY");

        int result = pushDao.addPush(push);
        assertEquals(1, result);

        List<Push> pushList = pushDao.getPushList(receiverId, 0, 20);
        boolean pushExists = pushList.stream()
                .anyMatch(p -> 
                    "INQUIRY_ANSWER".equals(p.getPushCode()) && 
                    "4".equals(p.getModuleId()) &&
                    "COMMUNITY".equals(p.getModuleType())
                );

        assertTrue(pushExists);
    }

    @Test
    @Order(10)
    @DisplayName("템플릿 파라미터 치환 로직 테스트")
    void testTemplateParamReplacement() {
        String template = "{nickname}님의 {amount}원 결제가 완료되었습니다.";
        Map<String, String> params = new HashMap<>();
        params.put("nickname", "사용자001");
        params.put("amount", "5000");

        String result = template;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        assertEquals("사용자001님의 5000원 결제가 완료되었습니다.", result);
    }
}