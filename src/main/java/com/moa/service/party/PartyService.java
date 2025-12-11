package com.moa.service.party;

import java.util.List;

import com.moa.dto.party.request.PartyCreateRequest;
import com.moa.dto.party.response.PartyDetailResponse;
import com.moa.dto.party.response.PartyListResponse;
import com.moa.dto.party.request.UpdateOttAccountRequest;
import com.moa.dto.partymember.response.PartyMemberResponse;
import com.moa.dto.payment.request.PaymentRequest;

/**
 * 파티 서비스 인터페이스
 * 파티 관련 비즈니스 로직 정의
 *
 * v1.0 핵심 프로세스:
 * 1. 파티 생성 (PENDING_PAYMENT)
 * 2. 방장 보증금 결제 (RECRUITING)
 * 3. 파티원 가입 + 통합 결제 (ACTIVE)
 */
public interface PartyService {

	/**
	 * 파티 생성
	 *
	 * v1.0 프로세스:
	 * 1. 파티 정보 입력 및 검증
	 * 2. PARTY 테이블 INSERT (상태: PENDING_PAYMENT)
	 * 3. PARTY_MEMBER 테이블 INSERT (방장, 상태: PENDING_PAYMENT)
	 *
	 * 주의:
	 * - 이 단계에서는 보증금 결제하지 않음
	 * - 별도로 processLeaderDeposit() 호출 필요
	 *
	 * @param userId  방장 사용자 ID (로그인 사용자)
	 * @param request 파티 생성 요청 (구독 ID, 최대 인원, 시작일, OTT 계정 등)
	 * @return 생성된 파티 상세 정보
	 */
	PartyDetailResponse createParty(String userId, PartyCreateRequest request);

	/**
	 * 방장 보증금 결제 처리
	 *
	 * v1.0 프로세스:
	 * 1. 보증금 금액 = 월구독료 전액
	 * 2. DEPOSIT 테이블 INSERT
	 * 3. PARTY_MEMBER 상태 → ACTIVE, depositId 연결
	 * 4. PARTY 상태 → RECRUITING
	 *
	 * @param partyId        파티 ID
	 * @param userId         방장 사용자 ID
	 * @param paymentRequest 결제 요청 정보 (Toss Payments 정보 포함)
	 * @return 업데이트된 파티 상세 정보
	 */
	PartyDetailResponse processLeaderDeposit(
			Integer partyId,
			String userId,
			PaymentRequest paymentRequest);

	/**
	 * 파티 상세 조회
	 * - 상품, 방장 정보 포함
	 * - 로그인하지 않은 경우에도 조회 가능
	 *
	 * @param partyId 파티 ID
	 * @return 파티 상세 정보
	 */
	PartyDetailResponse getPartyDetail(Integer partyId, String userId);

	/**
	 * 파티 목록 조회
	 * - 페이징 처리
	 * - 상품별, 상태별 필터링 가능
	 *
	 * @param productId   상품 ID (선택)
	 * @param partyStatus 파티 상태 (선택: RECRUITING, ACTIVE 등)
	 * @param keyword     검색 키워드 (선택)
	 * @param page        페이지 번호 (1부터 시작)
	 * @param size        페이지 크기
	 * @return 파티 목록
	 */
	List<PartyListResponse> getPartyList(
			Integer productId,
			String partyStatus,
			String keyword,
			java.time.LocalDate startDate,
			int page,
			int size,
			String sort);

	/**
	 * OTT 계정 정보 수정 (방장 전용)
	 * - 방장 권한 확인
	 * - OTT ID, Password 업데이트
	 *
	 * @param partyId 파티 ID
	 * @param userId  요청 사용자 ID (방장 확인용)
	 * @param request OTT 계정 정보
	 * @return 수정된 파티 상세 정보
	 */
	PartyDetailResponse updateOttAccount(
			Integer partyId,
			String userId,
			UpdateOttAccountRequest request);

	/**
	 * 파티 참여 (파티원 가입)
	 *
	 * v1.0 프로세스:
	 * 1. 파티 상태 확인 (RECRUITING만 가입 가능)
	 * 2. 정원 확인
	 * 3. 중복 가입 확인
	 * 4. PARTY_MEMBER 생성 (상태: PENDING_PAYMENT)
	 * 5. 보증금 결제 (인당 요금)
	 * 6. 첫 달 구독료 결제 (인당 요금)
	 * 7. DEPOSIT, PAYMENT 생성
	 * 8. PARTY_MEMBER 상태 → ACTIVE, depositId/firstPaymentId 연결
	 * 9. PARTY CURRENT_MEMBERS 증가
	 * 10. 최대 인원 도달 시 PARTY 상태 → ACTIVE
	 *
	 * 주의:
	 * - 보증금 + 첫 달 구독료를 동시에 결제
	 * - 결제 실패 시 전체 롤백 (트랜잭션)
	 *
	 * @param partyId        파티 ID
	 * @param userId         참여하는 사용자 ID
	 * @param paymentRequest 결제 요청 정보 (통합 결제 금액)
	 * @return 가입된 멤버 정보
	 */
	PartyMemberResponse joinParty(
			Integer partyId,
			String userId,
			PaymentRequest paymentRequest);

	/**
	 * 파티 멤버 목록 조회
	 * - 방장 포함 모든 멤버 조회
	 *
	 * @param partyId 파티 ID
	 * @return 파티 멤버 목록
	 */
	List<PartyMemberResponse> getPartyMembers(Integer partyId);

	/**
	 * 파티 탈퇴
	 *
	 * v1.0: 기능 제외 (v2.0에서 구현 예정)
	 * 호출 시 FEATURE_NOT_AVAILABLE 예외 발생
	 *
	 * @param partyId 파티 ID
	 * @param userId  탈퇴하는 사용자 ID
	 */
	void leaveParty(Integer partyId, String userId);

	/**
	 * 내가 참여 중인 모든 파티 목록 조회 (방장 + 파티원)
	 * - 내가 방장인 파티와 파티원인 파티 모두 포함
	 *
	 * @param userId 사용자 ID
	 * @return 파티 목록
	 */
	List<PartyListResponse> getMyParties(String userId);

	/**
	 * 내가 방장인 파티 목록 조회
	 *
	 * @param userId 사용자 ID
	 * @return 파티 목록
	 */
	List<PartyListResponse> getMyLeadingParties(String userId);

	/**
	 * 내가 멤버로 참여중인 파티 목록 조회 (방장 제외)
	 *
	 * @param userId 사용자 ID
	 * @return 파티 목록
	 */
	List<PartyListResponse> getMyParticipatingParties(String userId);

	/**
	 * 파티 종료 처리 (Scheduler용)
	 * 
	 * @param partyId 파티 ID
	 * @param reason  종료 사유
	 */
	void closeParty(Integer partyId, String reason);

	/**
	 * 만료된 파티 취소 처리 (Scheduler용)
	 * 
	 * @param partyId 파티 ID
	 * @param reason  취소 사유
	 */
	void cancelExpiredParty(Integer partyId, String reason);
}