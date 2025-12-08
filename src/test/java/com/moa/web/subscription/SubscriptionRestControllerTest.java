package com.moa.web.subscription;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.moa.dto.subscription.SubscriptionDTO;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
class SubscriptionRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static SubscriptionDTO testSubscription;
    private static Integer insertedSubscriptionId;

    @BeforeAll
    static void setUpAll() {
        testSubscription = new SubscriptionDTO();
        testSubscription.setUserId("user001@gmail.com"); // 실제 존재하는 사용자 ID로 수정 필요
        testSubscription.setProductId(1); // 실제 존재하는 상품 ID로 수정 필요
        testSubscription.setSubscriptionStatus("ACTIVE");
        testSubscription.setStartDate(Date.valueOf("2025-01-01"));
        testSubscription.setEndDate(null);
        testSubscription.setCancelReason(null);
        testSubscription.setCancelDate(null);
    }

    @Test
    @Order(1)
    @DisplayName("구독 등록 테스트")
    @Rollback(false)
    void testAddSubscription() throws Exception {
        mockMvc.perform(post("/api/subscription")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testSubscription)))
                .andDo(print())
                .andExpect(status().isOk());

        // 등록 후 목록 조회해서 방금 들어간 구독 찾기
        MvcResult listResult = mockMvc.perform(get("/api/subscription"))
                .andExpect(status().isOk())
                .andReturn();

        String json = listResult.getResponse().getContentAsString();
        // 첫 번째 구독(가장 최근)의 ID 저장
        insertedSubscriptionId = JsonPath.parse(json).read("$[0].subscriptionId");
        log.info("Inserted subscriptionId: {}", insertedSubscriptionId);
    }

    @Test
    @Order(2)
    @DisplayName("구독 단건 조회 테스트")
    void testGetSubscription() throws Exception {
        mockMvc.perform(get("/api/subscription/" + insertedSubscriptionId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscriptionId").value(insertedSubscriptionId))
                .andExpect(jsonPath("$.userId").value("user001@gmail.com"))
                .andExpect(jsonPath("$.subscriptionStatus").value("ACTIVE"));
    }

    @Test
    @Order(3)
    @DisplayName("구독 목록 조회 테스트")
    void testGetSubscriptionList() throws Exception {
        mockMvc.perform(get("/api/subscription"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    @Order(4)
    @DisplayName("구독 수정 테스트")
    @Rollback(false)
    void testUpdateSubscription() throws Exception {
        SubscriptionDTO updateSubscription = new SubscriptionDTO();
        updateSubscription.setSubscriptionId(insertedSubscriptionId);
        updateSubscription.setUserId("user001@gmail.com");
        updateSubscription.setProductId(1);
        updateSubscription.setSubscriptionStatus("PAUSED");
        updateSubscription.setStartDate(Date.valueOf("2025-01-01"));
        updateSubscription.setEndDate(Date.valueOf("2025-12-31"));
        updateSubscription.setCancelReason("테스트 취소");
        updateSubscription.setCancelDate(Date.valueOf("2025-02-01"));

        mockMvc.perform(put("/api/subscription")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateSubscription)))
                .andDo(print())
                .andExpect(status().isOk());

        // 수정 확인
        mockMvc.perform(get("/api/subscription/" + insertedSubscriptionId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscriptionStatus").value("PAUSED"));
    }

    @Test
    @Order(5)
    @DisplayName("구독 취소 테스트")
    @Rollback(false)
    void testCancelSubscription() throws Exception {
        mockMvc.perform(post("/api/subscription/" + insertedSubscriptionId + "/cancel"))
                .andDo(print())
                .andExpect(status().isOk());

        // 취소 확인 (상태가 CANCELED로 변경되었는지 등 확인 필요하지만, 현재 로직상 cancelSubscription이 상태를 변경하는지
        // 확인 필요)
        // 여기서는 호출 성공 여부만 확인
    }
}
