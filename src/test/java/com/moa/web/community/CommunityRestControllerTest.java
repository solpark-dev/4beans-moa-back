package com.moa.web.community;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moa.dao.community.CommunityDao;
import com.moa.dao.push.PushDao;
import com.moa.domain.Community;
import com.moa.domain.Push;
import com.moa.dto.community.request.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
//@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CommunityRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PushDao pushDao;

    @Autowired
    private CommunityDao communityDao;

    private static NoticeRequest testNoticeRequest;
    private static FaqRequest testFaqRequest;
    private static InquiryRequest testInquiryRequest;
    private static AnswerRequest testAnswerRequest;

    @BeforeAll
    static void setUpAll() {
        testNoticeRequest = NoticeRequest.builder()
                .userId("admin@moa.com")
                .communityCodeId(10)
                .title("테스트 공지사항 제목")
                .content("테스트 공지사항 내용입니다.")
                .build();

        testFaqRequest = FaqRequest.builder()
                .userId("admin@moa.com")
                .communityCodeId(4)
                .title("테스트 FAQ 제목")
                .content("테스트 FAQ 내용입니다.")
                .build();

        testInquiryRequest = InquiryRequest.builder()
                .userId("user002@naver.com")
                .communityCodeId(1)
                .title("테스트 문의 제목")
                .content("테스트 문의 내용입니다.")
                .build();

        testAnswerRequest = AnswerRequest.builder()
                .communityId(4)
                .answerContent("테스트 답변 내용입니다.")
                .build();
    }

    @Test
    @Order(1)
    @DisplayName("공지사항 목록 조회 테스트")
    void testGetNoticeList() throws Exception {
        mockMvc.perform(get("/api/community/notice")
                        .param("page", "1")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.total").isNumber());
    }

    @Test
    @Order(2)
    @DisplayName("공지사항 상세 조회 테스트")
    void testGetNotice() throws Exception {
        int communityId = 1;

        mockMvc.perform(get("/api/community/notice/" + communityId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.communityId").value(communityId))
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.content").exists());
    }

    @Test
    @Order(3)
    @DisplayName("공지사항 등록 테스트")
    void testAddNotice() throws Exception {
        mockMvc.perform(post("/api/community/notice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testNoticeRequest)))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @Order(4)
    @DisplayName("공지사항 수정 테스트")
    void testUpdateNotice() throws Exception {
        int communityId = 1;

        NoticeRequest updateRequest = NoticeRequest.builder()
                .userId("admin@moa.com")
                .communityCodeId(10)
                .title("수정된 공지사항 제목")
                .content("수정된 공지사항 내용입니다.")
                .build();

        mockMvc.perform(put("/api/community/notice/" + communityId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @Order(5)
    @DisplayName("공지사항 검색 테스트")
    void testSearchNotice() throws Exception {
        mockMvc.perform(get("/api/community/notice/search")
                        .param("keyword", "시스템")
                        .param("page", "1")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @Order(6)
    @DisplayName("FAQ 목록 조회 테스트")
    void testGetFaqList() throws Exception {
        mockMvc.perform(get("/api/community/faq")
                        .param("page", "1")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.total").isNumber());
    }

    @Test
    @Order(7)
    @DisplayName("FAQ 상세 조회 테스트")
    void testGetFaq() throws Exception {
        int communityId = 2;

        mockMvc.perform(get("/api/community/faq/" + communityId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.communityId").value(communityId))
                .andExpect(jsonPath("$.title").exists());
    }

    @Test
    @Order(8)
    @DisplayName("FAQ 등록 테스트")
    void testAddFaq() throws Exception {
        mockMvc.perform(post("/api/community/faq")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testFaqRequest)))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @Order(9)
    @DisplayName("FAQ 수정 테스트")
    void testUpdateFaq() throws Exception {
        int communityId = 2;

        FaqRequest updateRequest = FaqRequest.builder()
                .userId("admin@moa.com")
                .communityCodeId(4)
                .title("수정된 FAQ 제목")
                .content("수정된 FAQ 내용입니다.")
                .build();

        mockMvc.perform(put("/api/community/faq/" + communityId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @Order(10)
    @DisplayName("FAQ 검색 테스트")
    void testSearchFaq() throws Exception {
        mockMvc.perform(get("/api/community/faq/search")
                        .param("keyword", "파티")
                        .param("page", "1")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @Order(11)
    @DisplayName("내 문의 목록 조회 테스트")
    void testGetMyInquiryList() throws Exception {
        String userId = "user001@gmail.com";

        mockMvc.perform(get("/api/community/inquiry/my")
                        .param("userId", userId)
                        .param("page", "1")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @Order(12)
    @DisplayName("전체 문의 목록 조회 테스트 (관리자)")
    void testGetInquiryList() throws Exception {
        mockMvc.perform(get("/api/community/inquiry")
                        .param("page", "1")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.total").isNumber());
    }

    @Test
    @Order(13)
    @DisplayName("문의 상세 조회 테스트")
    void testGetInquiry() throws Exception {
        int communityId = 4;

        mockMvc.perform(get("/api/community/inquiry/" + communityId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.communityId").value(communityId))
                .andExpect(jsonPath("$.title").exists());
    }

    @Test
    @Order(14)
    @DisplayName("문의 등록 테스트")
    void testAddInquiry() throws Exception {
        mockMvc.perform(post("/api/community/inquiry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testInquiryRequest)))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @Order(15)
    @DisplayName("답변 등록 시 Push 알림 발송 검증 테스트")
    void testAddAnswerWithPushNotification() throws Exception {
        int communityId = 4;
        String receiverId = "user001@gmail.com";
        
        System.out.println("\n========== 답변 등록 전 Push 개수 확인 ==========");
        List<Push> pushListBefore = pushDao.getPushList(receiverId, 0, 100);
        int pushCountBefore = pushListBefore.size();
        System.out.println("답변 등록 전 Push 개수: " + pushCountBefore);
        
        System.out.println("\n========== 답변 등록 요청 ==========");
        mockMvc.perform(post("/api/community/inquiry/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testAnswerRequest)))
                .andDo(print())
                .andExpect(status().isOk());
        
        System.out.println("\n========== 답변 등록 후 Push 검증 ==========");
        List<Push> pushListAfter = pushDao.getPushList(receiverId, 0, 100);
        int pushCountAfter = pushListAfter.size();
        System.out.println("답변 등록 후 Push 개수: " + pushCountAfter);
        
        assertEquals(pushCountBefore + 1, pushCountAfter, "답변 등록 후 Push가 1개 증가해야 함");
        
        Push sentPush = pushListAfter.stream()
                .filter(push -> 
                    "INQUIRY_ANSWER".equals(push.getPushCode()) && 
                    String.valueOf(communityId).equals(push.getModuleId()) &&
                    "COMMUNITY".equals(push.getModuleType())
                )
                .findFirst()
                .orElse(null);
        
        System.out.println("\n========== Push 발송 검증 ==========");
        assertNotNull(sentPush, "INQUIRY_ANSWER Push가 발송되어야 함");
        System.out.println("Push ID: " + sentPush.getPushId());
        System.out.println("수신자: " + sentPush.getReceiverId());
        System.out.println("Push Code: " + sentPush.getPushCode());
        System.out.println("제목: " + sentPush.getTitle());
        System.out.println("내용: " + sentPush.getContent());
        System.out.println("Module ID: " + sentPush.getModuleId());
        System.out.println("Module Type: " + sentPush.getModuleType());
        System.out.println("발송 시간: " + sentPush.getSentAt());
        System.out.println("읽음 여부: " + sentPush.getIsRead());
        
        assertEquals(receiverId, sentPush.getReceiverId());
        assertEquals("INQUIRY_ANSWER", sentPush.getPushCode());
        assertEquals(String.valueOf(communityId), sentPush.getModuleId());
        assertEquals("COMMUNITY", sentPush.getModuleType());
        assertEquals("N", sentPush.getIsRead());
        assertNotNull(sentPush.getTitle());
        assertNotNull(sentPush.getContent());
        assertNotNull(sentPush.getSentAt());
        
        assertTrue(sentPush.getTitle().contains("문의 답변 완료"), "제목에 '문의 답변 완료'가 포함되어야 함");
        assertTrue(sentPush.getContent().contains("답변이 등록되었습니다"), "내용에 '답변이 등록되었습니다'가 포함되어야 함");
        
        System.out.println("\n========== 답변 시간과 Push 발송 시간 비교 ==========");
        Community inquiry = communityDao.getInquiry(communityId);
        assertNotNull(inquiry.getAnsweredAt(), "답변 시간이 기록되어야 함");
        System.out.println("답변 시간: " + inquiry.getAnsweredAt());
        System.out.println("Push 발송 시간: " + sentPush.getSentAt());
        
        long timeDiff = Math.abs(Duration.between(inquiry.getAnsweredAt(), sentPush.getSentAt()).getSeconds());
        System.out.println("시간 차이: " + timeDiff + "초");
        
        assertTrue(timeDiff <= 5, "답변 시간과 Push 발송 시간이 5초 이내여야 함 (실제: " + timeDiff + "초)");
        
        System.out.println("\n========== Push 발송 검증 완료 ==========\n");
    }

    @Test
    @Order(16)
    @DisplayName("답변 수정 테스트")
    void testUpdateAnswer() throws Exception {
        AnswerRequest updateRequest = AnswerRequest.builder()
                .communityId(4)
                .answerContent("수정된 답변 내용입니다.")
                .build();

        mockMvc.perform(put("/api/community/inquiry/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isOk());
    }
}