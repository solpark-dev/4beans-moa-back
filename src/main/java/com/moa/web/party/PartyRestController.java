package com.moa.web.party;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moa.common.exception.ApiResponse;
import com.moa.common.exception.BusinessException;
import com.moa.common.exception.ErrorCode;
import com.moa.dto.party.request.PartyCreateRequest;
import com.moa.dto.party.response.PartyDetailResponse;
import com.moa.dto.party.response.PartyListResponse;
import com.moa.dto.party.request.UpdateOttAccountRequest;
import com.moa.dto.partymember.response.PartyMemberResponse;
import com.moa.dto.payment.request.PaymentRequest;
import com.moa.service.party.PartyService;

import jakarta.validation.Valid;

import java.util.List;

/**
 * 파티 관리 REST API Controller
 *
 * v1.0 구현 범위:
 * - 파티 생성 (PENDING_PAYMENT 상태)
 * - 방장 보증금 결제 (RECRUITING 전환)
 * - 파티 목록/상세 조회
 * - 파티 OTT 계정 정보 수정
 * - 파티원 가입 + 통합 결제 (보증금 + 첫 달)
 * - 파티 멤버 목록 조회
 * - 내가 가입한 파티 조회
 *
 * v1.0 제외:
 * - 파티 탈퇴 (v2.0)
 * - 파티 삭제 (v2.0)
 * - 멤버 강퇴 (v2.0)
 */
@RestController
@RequestMapping(value = "/api/parties", produces = "application/json; charset=UTF-8")
public class PartyRestController {

	private final PartyService partyService;

	public PartyRestController(PartyService partyService) {
		this.partyService = partyService;
	}

	private String getCurrentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return null;
		}
		Object principal = authentication.getPrincipal();
		if (principal == null || "anonymousUser".equals(principal)) {
			return null;
		}
		return authentication.getName();
	}

	// ========================================
	// 파티 생성 및 보증금 결제
	// ========================================

	/**
	 * 파티 생성
	 * POST /api/parties
	 *
	 * v1.0 프로세스:
	 * 1. 파티 정보 입력 (구독, 인원, 시작일, OTT 계정 등)
	 * 2. PARTY 테이블 INSERT (상태: PENDING_PAYMENT)
	 * 3. PARTY_MEMBER 테이블 INSERT (방장, 상태: PENDING_PAYMENT)
	 * 4. 이후 별도로 방장 보증금 결제 API 호출 필요
	 *
	 * @param request 파티 생성 요청
	 * @return 생성된 파티 상세 정보
	 */
	@PostMapping
	public ApiResponse<PartyDetailResponse> createParty(@Valid @RequestBody PartyCreateRequest request) {
		String userId = getCurrentUserId();
		if (userId == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		PartyDetailResponse response = partyService.createParty(userId, request);
		return ApiResponse.success(response);
	}

	/**
	 * 방장 보증금 결제
	 * POST /api/parties/{partyId}/leader-deposit
	 *
	 * v1.0 프로세스:
	 * 1. 보증금 금액 = 월구독료 전액 (예: Netflix 13,000원)
	 * 2. Toss Payments 결제 처리
	 * 3. DEPOSIT 테이블 INSERT
	 * 4. PARTY_MEMBER 상태 → ACTIVE
	 * 5. PARTY 상태 → RECRUITING
	 *
	 * @param partyId        파티 ID
	 * @param paymentRequest 결제 정보 (amount, tossPaymentKey 등)
	 * @return 업데이트된 파티 상세 정보
	 */
	@PostMapping("/{partyId}/leader-deposit")
	public ApiResponse<PartyDetailResponse> processLeaderDeposit(
			@PathVariable Integer partyId,
			@Valid @RequestBody PaymentRequest paymentRequest) {

		String userId = getCurrentUserId();
		if (userId == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		PartyDetailResponse response = partyService.processLeaderDeposit(partyId, userId, paymentRequest);
		return ApiResponse.success(response);
	}

	// ========================================
	// 파티 조회
	// ========================================

	/**
	 * 파티 목록 조회 (검색/필터링/페이징)
	 * GET /api/parties
	 *
	 * @param productId   상품 ID (선택)
	 * @param partyStatus 파티 상태 (선택: PENDING_PAYMENT, RECRUITING, ACTIVE, CLOSED)
	 * @param keyword     검색 키워드 (선택)
	 * @param page        페이지 번호 (기본값: 1)
	 * @param size        페이지 크기 (기본값: 10)
	 * @return 파티 목록
	 */
	@GetMapping
	public ApiResponse<List<PartyListResponse>> getPartyList(
			@RequestParam(required = false) Integer productId,
			@RequestParam(required = false) String partyStatus,
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd") java.time.LocalDate startDate,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "latest") String sort) {

		List<PartyListResponse> response = partyService.getPartyList(
				productId, partyStatus, keyword, startDate, page, size, sort);
		return ApiResponse.success(response);
	}

	/**
	 * 파티 상세 조회
	 * GET /api/parties/{partyId}
	 *
	 * @param partyId 파티 ID
	 * @return 파티 상세 정보 (상품 정보, 멤버 수 등 포함)
	 */
	@GetMapping("/{partyId}")
	public ApiResponse<PartyDetailResponse> getPartyDetail(@PathVariable Integer partyId) {
		String userId = getCurrentUserId();
		// 로그인하지 않은 사용자도 조회 가능 (userId가 null일 수 있음)
		PartyDetailResponse response = partyService.getPartyDetail(partyId, userId);
		return ApiResponse.success(response);
	}

	/**
	 * OTT 계정 정보 수정 (방장 전용)
	 * PATCH /api/parties/{partyId}/ott-account
	 *
	 * @param partyId 파티 ID
	 * @param request OTT 계정 정보 (ottId, ottPassword)
	 * @return 수정된 파티 상세 정보
	 */
	@PatchMapping("/{partyId}/ott-account")
	public ApiResponse<PartyDetailResponse> updateOttAccount(
			@PathVariable Integer partyId,
			@Valid @RequestBody UpdateOttAccountRequest request) {

		String userId = getCurrentUserId();
		if (userId == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		PartyDetailResponse response = partyService.updateOttAccount(partyId, userId, request);
		return ApiResponse.success(response);
	}

	// ========================================
	// 파티원 가입 및 결제
	// ========================================

	/**
	 * 파티 가입 (파티원 통합 결제)
	 * POST /api/parties/{partyId}/join
	 *
	 * v1.0 프로세스:
	 * 1. 파티 상태 확인 (RECRUITING만 가능)
	 * 2. 정원 확인
	 * 3. 중복 가입 확인
	 * 4. 통합 결제 처리:
	 * - 보증금: 인당 요금 (예: 3,250원)
	 * - 첫 달 구독료: 인당 요금 (예: 3,250원)
	 * - 총 결제 금액: 6,500원
	 * 5. DEPOSIT, PAYMENT 생성
	 * 6. PARTY_MEMBER 상태 → ACTIVE
	 * 7. PARTY CURRENT_MEMBERS 증가
	 * 8. 최대 인원 도달 시 PARTY 상태 → ACTIVE
	 *
	 * @param partyId        가입할 파티 ID
	 * @param paymentRequest 결제 정보 (통합 결제 금액)
	 * @return 가입된 멤버 정보
	 */
	@PostMapping("/{partyId}/join")
	public ApiResponse<PartyMemberResponse> joinParty(
			@PathVariable Integer partyId,
			@Valid @RequestBody PaymentRequest paymentRequest) {

		String userId = getCurrentUserId();
		if (userId == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		PartyMemberResponse response = partyService.joinParty(partyId, userId, paymentRequest);
		return ApiResponse.success(response);
	}

	/**
	 * 파티 멤버 목록 조회
	 * GET /api/parties/{partyId}/members
	 *
	 * @param partyId 파티 ID
	 * @return 파티 멤버 목록 (방장 포함)
	 */
	@GetMapping("/{partyId}/members")
	public ApiResponse<List<PartyMemberResponse>> getPartyMembers(@PathVariable Integer partyId) {
		List<PartyMemberResponse> response = partyService.getPartyMembers(partyId);
		return ApiResponse.success(response);
	}

	/**
	 * 파티 탈퇴
	 * DELETE /api/parties/{partyId}/leave
	 *
	 * v1.0: 미구현 (FEATURE_NOT_AVAILABLE 예외 발생)
	 * v2.0에서 구현 예정
	 *
	 * @param partyId 탈퇴할 파티 ID
	 * @return 성공 여부
	 */
	@DeleteMapping("/{partyId}/leave")
	public ApiResponse<Void> leaveParty(@PathVariable Integer partyId) {
		String userId = getCurrentUserId();
		if (userId == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		partyService.leaveParty(partyId, userId);
		return ApiResponse.success(null);
	}

	// ========================================
	// 사용자별 파티 조회
	// ========================================

	/**
	 * 내가 가입한 모든 파티 조회 (방장 + 멤버)
	 * GET /api/parties/my
	 *
	 * @return 내가 가입한 파티 목록
	 */
	@GetMapping("/my")
	public ApiResponse<List<PartyListResponse>> getMyParties() {
		String userId = getCurrentUserId();
		if (userId == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		List<PartyListResponse> response = partyService.getMyParties(userId);
		return ApiResponse.success(response);
	}

	/**
	 * 내가 방장인 파티 목록 조회
	 * GET /api/parties/my/leading
	 *
	 * @return 내가 방장인 파티 목록
	 */
	@GetMapping("/my/leading")
	public ApiResponse<List<PartyListResponse>> getMyLeadingParties() {
		String userId = getCurrentUserId();
		if (userId == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		List<PartyListResponse> response = partyService.getMyLeadingParties(userId);
		return ApiResponse.success(response);
	}

	/**
	 * 내가 멤버로 참여중인 파티 목록 조회 (방장 제외)
	 * GET /api/parties/my/participating
	 *
	 * @return 내가 멤버로 참여중인 파티 목록
	 */
	@GetMapping("/my/participating")
	public ApiResponse<List<PartyListResponse>> getMyParticipatingParties() {
		String userId = getCurrentUserId();
		if (userId == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		List<PartyListResponse> response = partyService.getMyParticipatingParties(userId);
		return ApiResponse.success(response);
	}
}