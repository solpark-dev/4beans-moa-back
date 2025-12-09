package com.moa.service.party.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.moa.common.exception.BusinessException;
import com.moa.common.exception.ErrorCode;
import com.moa.dao.party.PartyDao;
import com.moa.dao.partymember.PartyMemberDao;
import com.moa.dao.product.ProductDao;
import com.moa.domain.Party;
import com.moa.domain.PartyMember;
import com.moa.domain.Product;
import com.moa.domain.enums.MemberStatus;
import com.moa.domain.enums.PartyStatus;
import com.moa.dto.party.request.PartyCreateRequest;
import com.moa.dto.party.response.PartyDetailResponse;
import com.moa.dto.partymember.response.PartyMemberResponse;
import com.moa.dto.payment.request.PaymentRequest;
import com.moa.service.deposit.DepositService;
import com.moa.service.payment.PaymentService;

/**
 * PartyServiceImpl 단위 테스트
 * 
 * 테스트 실행 방법:
 * 1. IntelliJ: 클래스명 옆 초록 화살표 클릭 → Run
 * 2. Maven: mvn test -Dtest=PartyServiceImplTest
 * 
 * @ExtendWith(MockitoExtension.class): Mockito 사용 설정
 * 
 * @Mock: 가짜 객체 생성 (실제 DB 접근 안함)
 * @InjectMocks: Mock 객체들을 자동 주입
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("파티 서비스 테스트")
class PartyServiceImplTest {

	@Mock
	private PartyDao partyDao;

	@Mock
	private PartyMemberDao partyMemberDao;

	@Mock
	private ProductDao productDao;

	@Mock
	private DepositService depositService;

	@Mock
	private PaymentService paymentService;

	@Mock
	private com.moa.service.push.PushService pushService;

	@Mock
	private com.moa.service.payment.TossPaymentService tossPaymentService;

	@InjectMocks
	private PartyServiceImpl partyService;

	private String testUserId;
	private PartyCreateRequest createRequest;
	private Party testParty;
	private Product testProduct;

	/**
	 * 각 테스트 실행 전에 공통 데이터 초기화
	 */
	@BeforeEach
	void setUp() {
		testUserId = "test-user-123";

		// 파티 생성 요청 데이터
		createRequest = PartyCreateRequest.builder()
				.productId(1)
				.maxMembers(4)
				.startDate(LocalDate.now().plusDays(1))
				.ottId("test@ott.com")
				.ottPassword("password")
				.build();

		// 테스트용 상품 데이터
		testProduct = new Product();
		testProduct.setProductId(1);
		testProduct.setProductName("Netflix Premium");
		testProduct.setPrice(17000);

		// 테스트용 파티 데이터
		testParty = Party.builder()
				.partyId(1)
				.productId(1)
				.partyLeaderId(testUserId)
				.partyStatus(PartyStatus.RECRUITING)
				.maxMembers(4)
				.currentMembers(1)
				.monthlyFee(17000)
				.regDate(LocalDate.now().atStartOfDay())
				.startDate(LocalDate.now().plusDays(1).atStartOfDay())
				.build();
	}

	// ========================================
	// 파티 생성 테스트
	// ========================================

	@Test
	@DisplayName("파티 생성 성공")
	void createParty_Success() throws Exception {
		// given: 테스트 데이터 준비
		PartyDetailResponse expectedResponse = PartyDetailResponse.builder()
				.partyId(1)
				.productId(1)
				.partyLeaderId(testUserId)
				.partyStatus("RECRUITING")
				.maxMembers(4)
				.currentMembers(1)
				.monthlyFee(17000)
				.build();

		// Mock 동작 정의
		given(productDao.getProduct(1)).willReturn(testProduct);
		given(partyDao.insertParty(any(Party.class))).willAnswer(invocation -> {
			Party party = invocation.getArgument(0);
			party.setPartyId(1); // PK 자동 생성 시뮬레이션
			return 1;
		});
		given(partyDao.findDetailById(1)).willReturn(Optional.of(expectedResponse));

		// when: 실제 테스트 실행
		PartyDetailResponse result = partyService.createParty(testUserId, createRequest);

		// then: 결과 검증
		assertThat(result).isNotNull();
		assertThat(result.getPartyId()).isEqualTo(1);
		assertThat(result.getPartyLeaderId()).isEqualTo(testUserId);
		assertThat(result.getMaxMembers()).isEqualTo(4);
		assertThat(result.getCurrentMembers()).isEqualTo(1);

		// Mock 메서드 호출 검증
		then(productDao).should(times(1)).getProduct(1);
		then(partyDao).should(times(1)).insertParty(any(Party.class));
		then(partyDao).should(times(1)).findDetailById(1);
	}

	@Test
	@DisplayName("파티 생성 실패 - productId null")
	void createParty_Fail_NullProductId() {
		// given
		PartyCreateRequest invalidRequest = PartyCreateRequest.builder()
				.productId(null)
				.maxMembers(4)
				.startDate(LocalDate.now().plusDays(1))
				.ottId("test@ott.com")
				.ottPassword("password")
				.build();

		// when & then: 예외 발생 검증
		assertThatThrownBy(() -> partyService.createParty(testUserId, invalidRequest))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_ID_REQUIRED);
	}

	@Test
	@DisplayName("파티 생성 실패 - 잘못된 정원 (1명)")
	void createParty_Fail_InvalidMaxMembers() {
		// given
		PartyCreateRequest invalidRequest = PartyCreateRequest.builder()
				.productId(1)
				.maxMembers(1) // 최소 2명
				.startDate(LocalDate.now().plusDays(1))
				.ottId("test@ott.com")
				.ottPassword("password")
				.build();

		// when & then
		assertThatThrownBy(() -> partyService.createParty(testUserId, invalidRequest))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_MAX_MEMBERS);
	}

	// ========================================
	// 파티 조회 테스트
	// ========================================

	@Test
	@DisplayName("파티 상세 조회 성공")
	void getPartyDetail_Success() {
		// given
		Integer partyId = 1;
		String userId = "test-user";
		PartyDetailResponse expectedResponse = PartyDetailResponse.builder()
				.partyId(partyId)
				.productId(1)
				.partyStatus("RECRUITING")
				.partyLeaderId(userId)  // 방장 ID 추가
				.build();

		given(partyDao.findDetailById(partyId)).willReturn(Optional.of(expectedResponse));

		// when
		PartyDetailResponse result = partyService.getPartyDetail(partyId, userId);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getPartyId()).isEqualTo(partyId);
		then(partyDao).should(times(1)).findDetailById(partyId);
	}

	@Test
	@DisplayName("파티 상세 조회 실패 - 존재하지 않는 파티")
	void getPartyDetail_Fail_NotFound() {
		// given
		Integer partyId = 999;
		String userId = "test-user";
		given(partyDao.findDetailById(partyId)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> partyService.getPartyDetail(partyId, userId))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.PARTY_NOT_FOUND);
	}

	// ========================================
	// 파티 가입 테스트
	// ========================================

	@Test
	@DisplayName("파티 가입 성공")
	void joinParty_Success() {
		// given
		Integer partyId = 1;
		String joinUserId = "join-user-456";
		PaymentRequest paymentRequest = new PaymentRequest();
		paymentRequest.setTossPaymentKey("test-payment-key");
		paymentRequest.setOrderId("test-order-id");

		PartyMemberResponse expectedResponse = PartyMemberResponse.builder()
				.partyMemberId(1)
				.partyId(partyId)
				.userId(joinUserId)
				.memberRole("MEMBER")
				.memberStatus("ACTIVE")
				.build();

		given(partyDao.findById(partyId)).willReturn(Optional.of(testParty));
		given(partyMemberDao.findByPartyIdAndUserId(partyId, joinUserId))
				.willReturn(Optional.empty());
		given(partyMemberDao.insertPartyMember(any(PartyMember.class)))
				.willAnswer(invocation -> {
					PartyMember member = invocation.getArgument(0);
					member.setPartyMemberId(1);
					return 1;
				});
		given(partyMemberDao.findByPartyMemberId(1))
				.willReturn(Optional.of(expectedResponse));

		// Mock Toss Payment confirmation (새로 추가)
		willDoNothing().given(tossPaymentService).confirmPayment(anyString(), anyString(), anyInt());

		// Mock deposit and payment service calls (WithoutConfirm 메서드 사용)
		given(depositService.createDepositWithoutConfirm(anyInt(), anyInt(), anyString(), anyInt(), any(PaymentRequest.class)))
				.willReturn(com.moa.domain.Deposit.builder().depositId(100).build());
		given(paymentService.createInitialPaymentWithoutConfirm(anyInt(), anyInt(), anyString(), anyInt(), anyString(),
				any(PaymentRequest.class)))
				.willReturn(com.moa.domain.Payment.builder().paymentId(200).build());

		given(partyDao.incrementCurrentMembers(partyId)).willReturn(1);

		// when
		PartyMemberResponse result = partyService.joinParty(partyId, joinUserId, paymentRequest);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getUserId()).isEqualTo(joinUserId);
		assertThat(result.getMemberRole()).isEqualTo("MEMBER");

		then(tossPaymentService).should(times(1)).confirmPayment(anyString(), anyString(), anyInt());
		then(partyDao).should(times(1)).incrementCurrentMembers(partyId);
	}

	@Test
	@DisplayName("파티 가입 실패 - 방장은 파티원으로 참여 불가")
	void joinParty_Fail_LeaderCannotJoin() {
		// given
		Integer partyId = 1;
		String leaderUserId = testUserId; // 방장과 동일
		PaymentRequest paymentRequest = new PaymentRequest();

		given(partyDao.findById(partyId)).willReturn(Optional.of(testParty));

		// when & then
		assertThatThrownBy(() -> partyService.joinParty(partyId, leaderUserId, paymentRequest))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.LEADER_CANNOT_JOIN);
	}

	@Test
	@DisplayName("파티 가입 실패 - 이미 참여 중")
	void joinParty_Fail_AlreadyJoined() {
		// given
		Integer partyId = 1;
		String joinUserId = "join-user-456";
		PaymentRequest paymentRequest = new PaymentRequest();

		PartyMember existingMember = PartyMember.builder()
				.partyMemberId(1)
				.partyId(partyId)
				.userId(joinUserId)
				.memberStatus(MemberStatus.ACTIVE)
				.build();

		given(partyDao.findById(partyId)).willReturn(Optional.of(testParty));
		given(partyMemberDao.findByPartyIdAndUserId(partyId, joinUserId))
				.willReturn(Optional.of(existingMember));

		// when & then
		assertThatThrownBy(() -> partyService.joinParty(partyId, joinUserId, paymentRequest))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_JOINED);
	}

	@Test
	@DisplayName("파티 가입 실패 - 정원 초과")
	void joinParty_Fail_PartyFull() {
		// given
		Integer partyId = 1;
		String joinUserId = "join-user-456";
		PaymentRequest paymentRequest = new PaymentRequest();

		Party fullParty = Party.builder()
				.partyId(partyId)
				.partyLeaderId(testUserId)
				.partyStatus(PartyStatus.RECRUITING)
				.maxMembers(4)
				.currentMembers(4) // 정원 모두 참
				.build();

		given(partyDao.findById(partyId)).willReturn(Optional.of(fullParty));
		given(partyDao.incrementCurrentMembers(partyId)).willReturn(0); // 정원 증가 실패

		// when & then
		assertThatThrownBy(() -> partyService.joinParty(partyId, joinUserId, paymentRequest))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.PARTY_FULL);
	}

	// ========================================
	// 파티 탈퇴 테스트
	// ========================================

	@Test
	@DisplayName("파티 탈퇴 성공")
	void leaveParty_Success() {
		// given
		Integer partyId = 1;
		String leaveUserId = "member-user-789";

		PartyMember member = PartyMember.builder()
				.partyMemberId(2)
				.partyId(partyId)
				.userId(leaveUserId)
				.memberStatus(MemberStatus.ACTIVE)
				.depositId(100)
				.build();

		given(partyDao.findById(partyId)).willReturn(Optional.of(testParty));
		given(partyMemberDao.findByPartyIdAndUserId(partyId, leaveUserId)).willReturn(Optional.of(member));
		given(partyDao.decrementCurrentMembers(partyId)).willReturn(1);

		// when
		partyService.leaveParty(partyId, leaveUserId);

		// then
		then(partyMemberDao).should(times(1)).updatePartyMember(any(PartyMember.class));
		then(partyDao).should(times(1)).decrementCurrentMembers(partyId);
	}

	@Test
	@DisplayName("파티 탈퇴 실패 - 방장은 탈퇴 불가")
	void leaveParty_Fail_LeaderCannotLeave() {
		// given
		Integer partyId = 1;
		String leaderUserId = testUserId; // 방장

		given(partyDao.findById(partyId)).willReturn(Optional.of(testParty));

		// when & then
		assertThatThrownBy(() -> partyService.leaveParty(partyId, leaderUserId))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.LEADER_CANNOT_LEAVE);
	}
}
