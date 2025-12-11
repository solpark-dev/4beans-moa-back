package com.moa.web.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moa.dao.push.PushDao;
import com.moa.domain.Push;
import com.moa.dto.push.request.MultiPushRequest;
import com.moa.dto.push.request.PushRequest;
import com.moa.dto.push.request.TemplatePushRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
//@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PushRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PushDao pushDao;

    private static final String TEST_USER_ID = "user001@gmail.com";
    private static final String TEST_USER2_ID = "user002@naver.com";
    private static final String TEST_USER3_ID = "user003@daum.net";

    @Test
    @Order(1)
    @DisplayName("단일 푸시 발송 테스트 - addPush()")
    @WithMockUser(username = "admin@moa.com", roles = {"ADMIN"})
    void testAddPush() throws Exception {
        PushRequest request = PushRequest.builder()
                .receiverId(TEST_USER_ID)
                .pushCode("PAYMENT_SUCCESS")
                .title("테스트 알림 제목")
                .content("테스트 알림 내용입니다.")
                .moduleId("TEST_001")
                .moduleType("TEST")
                .build();

        mockMvc.perform(post("/api/push")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("푸시 알림이 발송되었습니다."));

        List<Push> pushList = pushDao.getMyPushList(TEST_USER_ID, 0, 100);
        boolean exists = pushList.stream()
                .anyMatch(p -> "TEST_001".equals(p.getModuleId()) && "TEST".equals(p.getModuleType()));
        assertTrue(exists);
    }

    @Test
    @Order(2)
    @DisplayName("다중 푸시 발송 테스트 - addPushMulti()")
    @WithMockUser(username = "admin@moa.com", roles = {"ADMIN"})
    void testAddPushMulti() throws Exception {
        int beforeCount1 = pushDao.getMyPushList(TEST_USER_ID, 0, 100).size();
        int beforeCount2 = pushDao.getMyPushList(TEST_USER2_ID, 0, 100).size();
        int beforeCount3 = pushDao.getMyPushList(TEST_USER3_ID, 0, 100).size();

        MultiPushRequest request = MultiPushRequest.builder()
                .receiverIds(List.of(TEST_USER_ID, TEST_USER2_ID, TEST_USER3_ID))
                .pushCode("PARTY_START")
                .title("파티 시작 알림")
                .content("파티가 시작되었습니다.")
                .moduleId("PARTY_001")
                .moduleType("PARTY")
                .build();

        mockMvc.perform(post("/api/push/multi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("푸시 알림이 일괄 발송되었습니다."));

        int afterCount1 = pushDao.getMyPushList(TEST_USER_ID, 0, 100).size();
        int afterCount2 = pushDao.getMyPushList(TEST_USER2_ID, 0, 100).size();
        int afterCount3 = pushDao.getMyPushList(TEST_USER3_ID, 0, 100).size();

        assertEquals(beforeCount1 + 1, afterCount1);
        assertEquals(beforeCount2 + 1, afterCount2);
        assertEquals(beforeCount3 + 1, afterCount3);
    }

    @Test
    @Order(3)
    @DisplayName("템플릿 푸시 발송 테스트 - addTemplatePush()")
    @WithMockUser(username = "admin@moa.com", roles = {"ADMIN"})
    void testAddTemplatePush() throws Exception {
        int beforeCount = pushDao.getMyPushList(TEST_USER_ID, 0, 100).size();

        TemplatePushRequest request = TemplatePushRequest.builder()
                .receiverId(TEST_USER_ID)
                .pushCode("INQUIRY_ANSWER")
                .params(Map.of("nickname", "사용자001"))
                .moduleId("INQUIRY_001")
                .moduleType("COMMUNITY")
                .build();

        mockMvc.perform(post("/api/push/template")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("템플릿 푸시 알림이 발송되었습니다."));

        int afterCount = pushDao.getMyPushList(TEST_USER_ID, 0, 100).size();
        assertEquals(beforeCount + 1, afterCount);

        Push sentPush = pushDao.getMyPushList(TEST_USER_ID, 0, 100).stream()
                .filter(p -> "INQUIRY_ANSWER".equals(p.getPushCode()) && "INQUIRY_001".equals(p.getModuleId()))
                .findFirst()
                .orElse(null);

        assertNotNull(sentPush);
        assertTrue(sentPush.getTitle().contains("문의 답변 완료"));
        assertTrue(sentPush.getContent().contains("사용자001"));
    }

    @Test
    @Order(4)
    @DisplayName("푸시 상세 조회 테스트 - getPush()")
    @WithMockUser(username = "user001@gmail.com")
    void testGetPush() throws Exception {
        int pushId = 1;

        mockMvc.perform(get("/api/push/" + pushId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pushId").value(pushId))
                .andExpect(jsonPath("$.receiverId").exists())
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.content").exists());
    }

    @Test
    @Order(5)
    @DisplayName("전체 푸시 목록 조회 테스트 (관리자용) - getPushList()")
    @WithMockUser(username = "admin@moa.com", roles = {"ADMIN"})
    void testGetPushList() throws Exception {
        mockMvc.perform(get("/api/push/list")
                        .param("page", "1")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.total").isNumber());
    }

    @Test
    @Order(6)
    @DisplayName("내 푸시 목록 조회 테스트 - getMyPushList()")
    @WithMockUser(username = "user001@gmail.com")
    void testGetMyPushList() throws Exception {
        mockMvc.perform(get("/api/push/my")
                        .param("page", "1")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.total").isNumber());
    }

    @Test
    @Order(7)
    @DisplayName("안 읽은 푸시 개수 조회 테스트 - getUnreadCount()")
    @WithMockUser(username = "user001@gmail.com")
    void testGetUnreadCount() throws Exception {
        mockMvc.perform(get("/api/push/unread-count"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.count").isNumber());
    }

//    @Test
//    @Order(8)
//    @DisplayName("푸시 읽음 처리 테스트 - updateRead()")
//    @WithMockUser(username = "user001@gmail.com")
//    void testUpdateRead() throws Exception {
//        int pushId = 3;
//
//        Push beforePush = pushDao.getPush(pushId);
//        assertEquals("N", beforePush.getIsRead());
//
//        mockMvc.perform(patch("/api/push/" + pushId + "/read"))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true))
//                .andExpect(jsonPath("$.message").value("읽음 처리되었습니다."));
//
//        Push afterPush = pushDao.getPush(pushId);
//        assertEquals("Y", afterPush.getIsRead());
//        assertNotNull(afterPush.getReadAt());
//    }

//    @Test
//    @Order(9)
//    @DisplayName("전체 읽음 처리 테스트 - updateAllRead()")
//    @WithMockUser(username = "user001@gmail.com")
//    void testUpdateAllRead() throws Exception {
//        int unreadCountBefore = pushDao.getUnreadCount(TEST_USER_ID);
//        assertTrue(unreadCountBefore > 0);
//
//        mockMvc.perform(patch("/api/push/read-all"))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true))
//                .andExpect(jsonPath("$.message").value("전체 읽음 처리되었습니다."));
//
//        int unreadCountAfter = pushDao.getUnreadCount(TEST_USER_ID);
//        assertEquals(0, unreadCountAfter);
//    }

//    @Test
//    @Order(10)
//    @DisplayName("푸시 삭제 테스트 - deletePush()")
//    @WithMockUser(username = "user001@gmail.com")
//    void testDeletePush() throws Exception {
//        int pushId = 2;
//
//        Push beforePush = pushDao.getPush(pushId);
//        assertEquals("N", beforePush.getIsDeleted());
//
//        mockMvc.perform(delete("/api/push/" + pushId))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true))
//                .andExpect(jsonPath("$.message").value("삭제되었습니다."));
//
//        Push afterPush = pushDao.getPush(pushId);
//        assertEquals("Y", afterPush.getIsDeleted());
//    }

//    @Test
//    @Order(11)
//    @DisplayName("전체 삭제 테스트 - deleteAllPushs()")
//    @WithMockUser(username = "user003@daum.net")
//    void testDeleteAllPushs() throws Exception {
//        PushRequest request = PushRequest.builder()
//                .receiverId(TEST_USER3_ID)
//                .pushCode("TEST")
//                .title("삭제 테스트용 알림")
//                .content("삭제 테스트용 내용")
//                .moduleId("DELETE_TEST")
//                .moduleType("TEST")
//                .build();
//        pushDao.addPush(request.toEntity());
//
//        int beforeCount = pushDao.getMyPushList(TEST_USER3_ID, 0, 100).size();
//        assertTrue(beforeCount > 0);
//
//        mockMvc.perform(delete("/api/push/all"))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true))
//                .andExpect(jsonPath("$.message").value("전체 삭제되었습니다."));
//
//        int afterCount = pushDao.getMyPushList(TEST_USER3_ID, 0, 100).size();
//        assertEquals(0, afterCount);
//    }
}