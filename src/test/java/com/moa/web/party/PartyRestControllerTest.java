package com.moa.web.party;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moa.dto.party.request.PartyCreateRequest;
import com.moa.dto.party.request.UpdateOttAccountRequest;
import com.moa.dto.party.response.PartyDetailResponse;
import com.moa.dto.party.response.PartyListResponse;
import com.moa.dto.partymember.response.PartyMemberResponse;
import com.moa.dto.payment.request.PaymentRequest;
import com.moa.service.party.PartyService;

import com.moa.config.WebConfig;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@WebMvcTest(controllers = PartyRestController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = WebConfig.class))
@DisplayName("PartyRestController 단위 테스트")
@SuppressWarnings("deprecation") // Spring Boot 3.4+ MockBean deprecation handling
class PartyRestControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private PartyService partyService;

	@Autowired
	private ObjectMapper objectMapper;

	private MockHttpSession session;
	private String testUserId = "test-user-123";

	@BeforeEach
	void setUp() {
		session = new MockHttpSession();
		session.setAttribute("userId", testUserId);
	}

	@Test
	@DisplayName("파티 생성 성공")
	void createParty_Success() throws Exception {
		// given
		PartyCreateRequest request = PartyCreateRequest.builder()
				.productId(1)
				.maxMembers(4)
				.startDate(LocalDate.now().plusDays(1))
				.ottId("test@ott.com")
				.ottPassword("password")
				.build();

		PartyDetailResponse response = PartyDetailResponse.builder()
				.partyId(1)
				.productId(1)
				.partyLeaderId(testUserId)
				.partyStatus("PENDING_PAYMENT")
				.build();

		given(partyService.createParty(eq(testUserId), any(PartyCreateRequest.class)))
				.willReturn(response);

		// when & then
		mockMvc.perform(post("/api/parties")
				.session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.partyId").value(1))
				.andExpect(jsonPath("$.data.partyStatus").value("PENDING_PAYMENT"));
	}

	@Test
	@DisplayName("방장 보증금 결제 성공")
	void processLeaderDeposit_Success() throws Exception {
		// given
		Integer partyId = 1;
		PaymentRequest request = PaymentRequest.builder()
				.amount(10000)
				.paymentMethod("CARD")
				.build();

		PartyDetailResponse response = PartyDetailResponse.builder()
				.partyId(partyId)
				.partyStatus("RECRUITING")
				.build();

		given(partyService.processLeaderDeposit(eq(partyId), eq(testUserId), any(PaymentRequest.class)))
				.willReturn(response);

		// when & then
		mockMvc.perform(post("/api/parties/{partyId}/leader-deposit", partyId)
				.session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.partyStatus").value("RECRUITING"));
	}

	@Test
	@DisplayName("파티 목록 조회 성공")
	void getPartyList_Success() throws Exception {
		// given
		PartyListResponse listResponse = PartyListResponse.builder()
				.partyId(1)
				.productId(1)
				.partyStatus("RECRUITING")
				.build();

		given(partyService.getPartyList(any(), any(), any(), anyInt(), anyInt()))
				.willReturn(List.of(listResponse));

		// when & then
		mockMvc.perform(get("/api/parties")
				.param("page", "1")
				.param("size", "10"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].partyId").value(1));
	}

	@Test
	@DisplayName("파티 상세 조회 성공")
	void getPartyDetail_Success() throws Exception {
		// given
		Integer partyId = 1;
		PartyDetailResponse response = PartyDetailResponse.builder()
				.partyId(partyId)
				.productId(1)
				.build();

		given(partyService.getPartyDetail(partyId)).willReturn(response);

		// when & then
		mockMvc.perform(get("/api/parties/{partyId}", partyId))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.partyId").value(partyId));
	}

	@Test
	@DisplayName("OTT 계정 정보 수정 성공")
	void updateOttAccount_Success() throws Exception {
		// given
		Integer partyId = 1;
		UpdateOttAccountRequest request = UpdateOttAccountRequest.builder()
				.ottId("new@ott.com")
				.ottPassword("newPassword")
				.build();

		PartyDetailResponse response = PartyDetailResponse.builder()
				.partyId(partyId)
				.build();

		given(partyService.updateOttAccount(eq(partyId), eq(testUserId), any(UpdateOttAccountRequest.class)))
				.willReturn(response);

		// when & then
		mockMvc.perform(patch("/api/parties/{partyId}/ott-account", partyId)
				.session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("파티 가입 성공")
	void joinParty_Success() throws Exception {
		// given
		Integer partyId = 1;
		PaymentRequest request = PaymentRequest.builder()
				.amount(5000)
				.paymentMethod("CARD")
				.build();

		PartyMemberResponse response = PartyMemberResponse.builder()
				.partyId(partyId)
				.userId(testUserId)
				.memberStatus("ACTIVE")
				.build();

		given(partyService.joinParty(eq(partyId), eq(testUserId), any(PaymentRequest.class)))
				.willReturn(response);

		// when & then
		mockMvc.perform(post("/api/parties/{partyId}/join", partyId)
				.session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.memberStatus").value("ACTIVE"));
	}

	@Test
	@DisplayName("파티 멤버 목록 조회 성공")
	void getPartyMembers_Success() throws Exception {
		// given
		Integer partyId = 1;
		PartyMemberResponse memberResponse = PartyMemberResponse.builder()
				.partyId(partyId)
				.userId(testUserId)
				.build();

		given(partyService.getPartyMembers(partyId)).willReturn(List.of(memberResponse));

		// when & then
		mockMvc.perform(get("/api/parties/{partyId}/members", partyId))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].userId").value(testUserId));
	}

	@Test
	@DisplayName("내가 가입한 파티 조회 성공")
	void getMyParties_Success() throws Exception {
		// given
		PartyListResponse listResponse = PartyListResponse.builder()
				.partyId(1)
				.build();

		given(partyService.getMyParties(testUserId)).willReturn(List.of(listResponse));

		// when & then
		mockMvc.perform(get("/api/parties/my")
				.session(session))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].partyId").value(1));
	}
}
