package com.moa.service.party.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moa.common.exception.BusinessException;
import com.moa.common.exception.ErrorCode;
import com.moa.common.util.AESUtil; // Import AESUtil
import com.moa.dao.party.PartyDao;
import com.moa.dao.partymember.PartyMemberDao;
import com.moa.dao.product.ProductDao;
import com.moa.dao.user.UserDao;
import com.moa.dao.user.UserCardDao;
import com.moa.domain.Deposit;
import com.moa.domain.Party;
import com.moa.domain.PartyMember;
import com.moa.domain.Product;
import com.moa.domain.User;
import com.moa.domain.UserCard;
import com.moa.domain.enums.MemberStatus;
import com.moa.domain.enums.PartyStatus;
import com.moa.domain.enums.PushCodeType;
import com.moa.dto.party.request.PartyCreateRequest;
import com.moa.dto.party.request.UpdateOttAccountRequest;
import com.moa.dto.party.response.PartyDetailResponse;
import com.moa.dto.party.response.PartyListResponse;
import com.moa.dto.partymember.response.PartyMemberResponse;
import com.moa.dto.payment.request.PaymentRequest;
import com.moa.dto.push.request.TemplatePushRequest;
import com.moa.service.deposit.DepositService;
import com.moa.service.party.PartyService;
import com.moa.service.payment.PaymentService;
import com.moa.service.push.PushService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
public class PartyServiceImpl implements PartyService {

	private final PartyDao partyDao;
	private final PartyMemberDao partyMemberDao;
	private final ProductDao productDao;
	private final DepositService depositService;
	private final PaymentService paymentService;
	private final PushService pushService;
	private final com.moa.service.payment.TossPaymentService tossPaymentService;
	private final com.moa.service.refund.RefundRetryService refundRetryService;
	private final UserDao userDao;
	private final UserCardDao userCardDao;

	public PartyServiceImpl(PartyDao partyDao, PartyMemberDao partyMemberDao, ProductDao productDao,
			DepositService depositService, PaymentService paymentService, PushService pushService,
			com.moa.service.payment.TossPaymentService tossPaymentService,
			com.moa.service.refund.RefundRetryService refundRetryService, UserDao userDao, UserCardDao userCardDao) {
		this.partyDao = partyDao;
		this.partyMemberDao = partyMemberDao;
		this.productDao = productDao;
		this.depositService = depositService;
		this.paymentService = paymentService;
		this.pushService = pushService;
		this.tossPaymentService = tossPaymentService;
		this.refundRetryService = refundRetryService;
		this.userDao = userDao;
		this.userCardDao = userCardDao;
	}

	@Override
	public PartyDetailResponse createParty(String userId, PartyCreateRequest request) {
		validateCreateRequest(request);
		Product product = null;
		try {
			product = productDao.getProduct(request.getProductId());
		} catch (Exception e) {
		}
		if (product == null) {
			product = new Product();
			product.setProductId(request.getProductId());
			product.setProductName("Unknown Product");
			product.setPrice(10000);
		}

		int monthlyFee = product.getPrice() / request.getMaxMembers();

		Party party = Party.builder().productId(request.getProductId()).partyLeaderId(userId)
				.partyStatus(PartyStatus.PENDING_PAYMENT).maxMembers(request.getMaxMembers()).currentMembers(1)
				.monthlyFee(monthlyFee).ottId(request.getOttId()).ottPassword(AESUtil.encrypt(request.getOttPassword()))
				.accountId(request.getAccountId()).regDate(LocalDateTime.now())
				.startDate(request.getStartDate().atStartOfDay())
				.endDate(request.getEndDate() != null ? request.getEndDate().atStartOfDay() : null).build();

		partyDao.insertParty(party);

		PartyMember leaderMember = PartyMember.builder().partyId(party.getPartyId()).userId(userId).memberRole("LEADER")
				.memberStatus(MemberStatus.PENDING_PAYMENT).joinDate(LocalDateTime.now()).build();
		partyMemberDao.insertPartyMember(leaderMember);
		return partyDao.findDetailById(party.getPartyId())
				.orElseThrow(() -> new BusinessException(ErrorCode.PARTY_NOT_FOUND));
	}

	@Override
	@SuppressWarnings("unchecked")
	public PartyDetailResponse processLeaderDeposit(Integer partyId, String userId, PaymentRequest paymentRequest) {
		Party party = partyDao.findById(partyId).orElseThrow(() -> new BusinessException(ErrorCode.PARTY_NOT_FOUND));

		if (!party.getPartyLeaderId().equals(userId)) {
			throw new BusinessException(ErrorCode.NOT_PARTY_LEADER);
		}
		if (party.getPartyStatus() != PartyStatus.PENDING_PAYMENT) {
			throw new BusinessException(ErrorCode.INVALID_PARTY_STATUS);
		}

		int depositAmount = party.getMonthlyFee() * party.getMaxMembers();
		String paymentKey;
		String orderId;
		
		// Case 1: 일반 결제 (tossPaymentKey가 있는 경우) - 파티 생성 시 카드 미등록 사용자
		if (paymentRequest.getTossPaymentKey() != null && !paymentRequest.getTossPaymentKey().isEmpty()) {
			paymentKey = paymentRequest.getTossPaymentKey();
			orderId = paymentRequest.getOrderId();
			
			// 토스페이먼츠 일반 결제 승인
			tossPaymentService.confirmPayment(paymentKey, orderId, paymentRequest.getAmount());
			log.info("일반 결제 승인 완료: partyId={}, userId={}, paymentKey={}", partyId, userId, paymentKey);
		}
		// Case 2: 빌링키 결제 (기존 카드 사용 또는 새 카드 등록)
		else {
			String billingKey;
			
			// authKey가 있으면 새 빌링키 발급
			if (paymentRequest.getAuthKey() != null && !paymentRequest.getAuthKey().isEmpty()) {
				Map<String, Object> billingData = tossPaymentService.issueBillingKey(paymentRequest.getAuthKey(), userId);
				billingKey = (String) billingData.get("billingKey");
				Map<String, Object> cardInfo = (Map<String, Object>) billingData.get("card");

				UserCard newUserCard = UserCard.builder()
						.userId(userId)
						.billingKey(billingKey)
						.cardCompany((String) cardInfo.get("company"))
						.cardNumber((String) cardInfo.get("number"))
						.regDate(LocalDateTime.now())
						.build();
				userCardDao.findByUserId(userId).ifPresentOrElse(
						existingCard -> userCardDao.updateUserCard(newUserCard),
						() -> userCardDao.insertUserCard(newUserCard));
				log.info("새 빌링키 발급 완료: userId={}", userId);
			} else {
				// 기존 빌링키 사용 (재결제 또는 등록된 카드 사용)
				UserCard existingCard = userCardDao.findByUserId(userId)
						.orElseThrow(() -> new BusinessException(ErrorCode.BILLING_KEY_NOT_FOUND));
				billingKey = existingCard.getBillingKey();
				log.info("기존 빌링키 사용: userId={}", userId);
			}

			// 빌링키로 보증금 결제
			orderId = "DEPOSIT_" + partyId + "_" + userId + "_" + System.currentTimeMillis();
			paymentKey = tossPaymentService.payWithBillingKey(billingKey, orderId, depositAmount, "MOA 파티장 보증금", userId);
			log.info("빌링키 결제 완료: partyId={}, userId={}, paymentKey={}", partyId, userId, paymentKey);
		}

		// Deposit 기록 생성
		depositService.createDeposit(partyId, partyMemberDao.findByPartyIdAndUserId(partyId, userId).get().getPartyMemberId(), userId, depositAmount, paymentKey, orderId, "CARD");

		// 상태 변경
		PartyMember leaderMember = partyMemberDao.findByPartyIdAndUserId(partyId, userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.PARTY_MEMBER_NOT_FOUND));
		leaderMember.setMemberStatus(MemberStatus.ACTIVE);
		partyMemberDao.updatePartyMember(leaderMember);
		party.setPartyStatus(PartyStatus.RECRUITING);
		partyDao.updateParty(party);

		return partyDao.findDetailById(partyId).orElseThrow(() -> new BusinessException(ErrorCode.PARTY_NOT_FOUND));
	}

	@Override
	@Transactional(readOnly = true)
	public PartyDetailResponse getPartyDetail(Integer partyId, String userId) {
		PartyDetailResponse response = partyDao.findDetailById(partyId)
				.orElseThrow(() -> new BusinessException(ErrorCode.PARTY_NOT_FOUND));
		boolean isMember = false;
		if (userId != null) {
			if (response.getPartyLeaderId().equals(userId)) {
				isMember = true;
			} else {
				partyMemberDao.findByPartyIdAndUserId(partyId, userId).ifPresent(member -> {
					response.setMemberStatus(member.getMemberStatus());
				});

				if (response.getMemberStatus() == MemberStatus.ACTIVE) {
					isMember = true;
				}
			}
		}

		if (!isMember) {
			response.setOttId(null);
			response.setOttPassword(null);
		} else {
			response.setOttPassword(AESUtil.decrypt(response.getOttPassword()));
		}

		return response;
	}

	@Override
	@Transactional(readOnly = true)
	public List<PartyListResponse> getPartyList(Integer productId, String partyStatus, String keyword,
			java.time.LocalDate startDate, int page, int size, String sort) {

		// 상태 문자열을 Enum으로 변환
		PartyStatus status = null;
		if (partyStatus != null && !partyStatus.trim().isEmpty()) {
			try {
				status = PartyStatus.valueOf(partyStatus.toUpperCase());
			} catch (IllegalArgumentException e) {
				throw new BusinessException(ErrorCode.INVALID_PARTY_STATUS);
			}
		}

		// 페이지 번호 검증
		if (page < 1)
			page = 1;
		if (size <= 0)
			size = 10;

		// OFFSET 계산
		int offset = (page - 1) * size;

		return partyDao.findPartyList(productId, status, keyword, startDate, offset, size, sort);
	}

	@Override
	public PartyDetailResponse updateOttAccount(Integer partyId, String userId, UpdateOttAccountRequest request) {

		Party party = partyDao.findById(partyId).orElseThrow(() -> new BusinessException(ErrorCode.PARTY_NOT_FOUND));

		if (!party.getPartyLeaderId().equals(userId)) {
			throw new BusinessException(ErrorCode.NOT_PARTY_LEADER);
		}

		partyDao.updateOttAccount(partyId, request.getOttId(), AESUtil.encrypt(request.getOttPassword()));
		return partyDao.findDetailById(partyId).orElseThrow(() -> new BusinessException(ErrorCode.PARTY_NOT_FOUND));
	}

	@Override
	public PartyMemberResponse joinParty(Integer partyId, String userId, PaymentRequest paymentRequest) {

		// 1. 초기 검증 및 파티/멤버 기본 정보 설정
		Party party = partyDao.findById(partyId).orElseThrow(() -> new BusinessException(ErrorCode.PARTY_NOT_FOUND));

		if (party.getPartyStatus() != PartyStatus.RECRUITING) {
			throw new BusinessException(ErrorCode.PARTY_NOT_RECRUITING);
		}
		if (party.getPartyLeaderId().equals(userId)) {
			throw new BusinessException(ErrorCode.LEADER_CANNOT_JOIN);
		}
		partyMemberDao.findByPartyIdAndUserId(partyId, userId).ifPresent(member -> {
			throw new BusinessException(ErrorCode.ALREADY_JOINED);
		});

		// 정원 증가 시도. 실패하면 파티 정원 초과
		int updatedRows = partyDao.incrementCurrentMembers(partyId);
		if (updatedRows == 0) {
			throw new BusinessException(ErrorCode.PARTY_FULL);
		}

		// 파티 멤버 임시 생성 (결제 실패 시 트랜잭션 롤백에 의존)
		PartyMember partyMember = PartyMember.builder()
				.partyId(partyId)
				.userId(userId) // userId 필드 추가가 필요할 수 있음
				.memberRole("MEMBER")
				.memberStatus(MemberStatus.PENDING_PAYMENT)
				.joinDate(LocalDateTime.now())
				.build();
		partyMemberDao.insertPartyMember(partyMember); // 먼저 멤버를 생성하여 ID를 확보 (트랜잭션에 포함)

		// 2. 빌링키 처리 로직
		UserCard userCard;
		String billingKey;
		String cardCompany = null;
		String cardNumber = null; // 마지막 4자리

		Optional<UserCard> existingUserCard = userCardDao.findByUserId(userId);

		if (paymentRequest.getAuthKey() != null && !paymentRequest.getAuthKey().isEmpty()) {
			// Case A: 새 카드 등록 (authKey로 빌링키 발급 및 저장/업데이트)
			Map<String, Object> billingKeyIssueResponse = tossPaymentService.issueBillingKey(paymentRequest.getAuthKey(), userId);
			billingKey = (String) billingKeyIssueResponse.get("billingKey");
			Map<String, Object> cardInfo = (Map<String, Object>) billingKeyIssueResponse.get("card");
			cardCompany = (String) cardInfo.get("company");
			cardNumber = (String) cardInfo.get("number"); // 카드 마지막 4자리

			UserCard newOrUpdatedCard = UserCard.builder()
					.userId(userId)
					.billingKey(billingKey)
					.cardCompany(cardCompany)
					.cardNumber(cardNumber)
					.regDate(LocalDateTime.now())
					.build();

			if (existingUserCard.isPresent()) {
				userCardDao.updateUserCard(newOrUpdatedCard); // 기존 카드 정보 업데이트
				log.info("사용자 {}의 카드 정보 업데이트 완료 (billingKey: {})", userId, billingKey);
			} else {
				userCardDao.insertUserCard(newOrUpdatedCard); // 새 카드 정보 삽입
				log.info("사용자 {}의 카드 정보 등록 완료 (billingKey: {})", userId, billingKey);
			}
			userCard = newOrUpdatedCard;

		} else if (paymentRequest.isUseExistingCard()) {
			// Case B: 기존 카드 사용
			userCard = existingUserCard.orElseThrow(() -> new BusinessException(ErrorCode.BILLING_KEY_NOT_FOUND, "저장된 카드 정보가 없습니다."));
			billingKey = userCard.getBillingKey();
			log.info("사용자 {}의 기존 카드 정보 사용 (billingKey: {})", userId, billingKey);
		} else {
			// 유효하지 않은 요청 (authKey도 없고 기존 카드 사용 플래그도 없음)
			throw new BusinessException(ErrorCode.INVALID_PAYMENT_REQUEST, "유효한 결제 요청 정보가 없습니다.");
		}

		// 3. 빌링키로 첫 결제 실행 (보증금 + 월회비)
		int fee = party.getMonthlyFee();
		int depositAmount = fee; // 보증금은 월회비와 동일
		int initialSubscriptionFee = fee; // 첫 달 월회비
		int totalAmount = depositAmount + initialSubscriptionFee;

		String orderId = "PARTY_JOIN_" + party.getPartyId() + "_" + userId + "_" + System.currentTimeMillis();
		String paymentKey;
		try {
			paymentKey = tossPaymentService.payWithBillingKey(
					billingKey,
					orderId,
					totalAmount,
					"MOA 파티 가입 (보증금 + 첫 달 구독료)",
					userId // customerKey
			);
			log.info("Toss 빌링키 결제 성공: paymentKey={}, orderId={}", paymentKey, orderId);
		} catch (Exception e) {
			// 결제 실패 시, 트랜잭션 롤백 (partyMember 생성, currentMembers 증가 등 모두 롤백)
			log.error("Toss 빌링키 결제 실패: partyId={}, userId={}, error={}", partyId, userId, e.getMessage());
			throw e;
		}

		// 4. DEPOSIT 및 PAYMENT 기록 (두 가지 타입으로 분리)
		String targetMonth = party.getStartDate().format(DateTimeFormatter.ofPattern("yyyy-MM"));

		// 보증금 기록
		depositService.createDeposit(
				partyId,
				partyMember.getPartyMemberId(),
				userId,
				depositAmount,
				paymentKey, // paymentKey 전달
				orderId,
				"CARD" // 결제 수단
		);
		log.info("보증금 기록 완료: partyId={}, userId={}, amount={}", partyId, userId, depositAmount);

		// 첫 달 월회비 기록
		paymentService.createInitialSubscriptionPayment(
				partyId,
				partyMember.getPartyMemberId(),
				userId,
				initialSubscriptionFee,
				targetMonth,
				paymentKey, // paymentKey 전달
				orderId,
				"CARD" // 결제 수단
		);
		log.info("첫 달 월회비 기록 완료: partyId={}, userId={}, amount={}", partyId, userId, initialSubscriptionFee);

		// 5. 파티 멤버 최종 활성화 및 푸시 알림
		partyMember.setMemberStatus(MemberStatus.ACTIVE);
		partyMemberDao.updatePartyMember(partyMember);
		log.info("파티 멤버 {} 활성화 완료: partyId={}", userId, partyId);

		// 파티 정원 확인 및 상태 변경
		Party updatedParty = partyDao.findById(partyId)
				.orElseThrow(() -> new BusinessException(ErrorCode.PARTY_NOT_FOUND));

		if (updatedParty.getCurrentMembers() >= updatedParty.getMaxMembers()) {
			partyDao.updatePartyStatus(partyId, PartyStatus.ACTIVE);
			safeSendPush(() -> sendPartyStartPushToAllMembers(partyId, updatedParty));
			log.info("파티 {} 활성화 완료 (정원 충족)", partyId);
		}

		safeSendPush(() -> sendPartyJoinPush(userId, getUserNickname(userId), party));
		safeSendPush(() -> sendPartyMemberJoinPushToLeader(userId, party));

		return partyMemberDao.findByPartyMemberId(partyMember.getPartyMemberId())
				.orElseThrow(() -> new BusinessException(ErrorCode.PARTY_MEMBER_NOT_FOUND));
	}

	@Override
	@Transactional(readOnly = true)
	public List<PartyMemberResponse> getPartyMembers(Integer partyId) {
		partyDao.findById(partyId).orElseThrow(() -> new BusinessException(ErrorCode.PARTY_NOT_FOUND));

		return partyMemberDao.findMembersByPartyId(partyId);
	}

	@Override
	public void leaveParty(Integer partyId, String userId) {
		Party party = partyDao.findById(partyId).orElseThrow(() -> new BusinessException(ErrorCode.PARTY_NOT_FOUND));
		if (party.getPartyLeaderId().equals(userId)) {
			throw new BusinessException(ErrorCode.LEADER_CANNOT_LEAVE);
		}

		PartyMember member = partyMemberDao.findByPartyIdAndUserId(partyId, userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_PARTY_MEMBER));

		if (member.getMemberStatus() != MemberStatus.ACTIVE) {
			throw new BusinessException(ErrorCode.NOT_PARTY_MEMBER);
		}

		member.setMemberStatus(MemberStatus.INACTIVE);
		member.setWithdrawDate(LocalDateTime.now());
		partyMemberDao.updatePartyMember(member);

		int updatedRows = partyDao.decrementCurrentMembers(partyId);
		if (updatedRows == 0) {
			throw new BusinessException(ErrorCode.PARTY_NOT_FOUND);
		}

		Deposit memberDeposit = depositService.findByPartyIdAndUserId(partyId, userId);
		if (memberDeposit != null) {
			try {
				depositService.processWithdrawalRefund(memberDeposit.getDepositId(), party);
			} catch (Exception e) {
				log.error("보증금 처리 실패: {}", e.getMessage());
			}
		}

		if (party.getStartDate().isAfter(LocalDateTime.now())) {
			try {
				paymentService.refundPayment(partyId, member.getPartyMemberId(), "파티 시작 전 탈퇴 (구독료 환불)");
			} catch (Exception e) {
				log.error("구독료 환불 실패: {}", e.getMessage());
			}
		}
		Party updatedParty = partyDao.findById(partyId)
				.orElseThrow(() -> new BusinessException(ErrorCode.PARTY_NOT_FOUND));

		if (updatedParty.getPartyStatus() == PartyStatus.ACTIVE
				&& updatedParty.getCurrentMembers() < updatedParty.getMaxMembers()) {
			partyDao.updatePartyStatus(partyId, PartyStatus.RECRUITING);
		}

		safeSendPush(() -> sendPartyWithdrawPush(userId, party));
		safeSendPush(() -> sendPartyMemberWithdrawPushToLeader(userId, party));
	}

	@Override
	@Transactional(readOnly = true)
	public List<PartyListResponse> getMyParties(String userId, boolean includeClosed) {
		return partyDao.findMyParties(userId, includeClosed);
	}

	@Override
	@Transactional(readOnly = true)
	public List<PartyListResponse> getMyLeadingParties(String userId, boolean includeClosed) {
		return partyDao.findMyLeadingParties(userId, includeClosed);
	}

	@Override
	@Transactional(readOnly = true)
	public List<PartyListResponse> getMyParticipatingParties(String userId, boolean includeClosed) {
		return partyDao.findMyParticipatingParties(userId, includeClosed);
	}

	@Override
	@Transactional(readOnly = true)
	public List<PartyListResponse> getMyClosedParties(String userId) {
		return partyDao.findMyClosedParties(userId);
	}

	// ========== Private 검증 메서드 ==========

	private void validateCreateRequest(PartyCreateRequest request) {
		if (request.getProductId() == null) {
			throw new BusinessException(ErrorCode.PRODUCT_ID_REQUIRED);
		}
		if (request.getMaxMembers() == null || request.getMaxMembers() < 2 || request.getMaxMembers() > 10) {
			throw new BusinessException(ErrorCode.INVALID_MAX_MEMBERS);
		}
		if (request.getStartDate() == null) {
			throw new BusinessException(ErrorCode.START_DATE_REQUIRED);
		}
	}

	// ========== ⭐ Private Push 메서드 ==========

	private void safeSendPush(Runnable pushAction) {
		try {
			pushAction.run();
		} catch (Exception e) {
			log.error("Push 발송 실패 (무시): {}", e.getMessage());
		}
	}

	private String getUserNickname(String userId) {
		if (userId == null)
			return "파티원";

		try {
			return userDao.findByUserId(userId).map(User::getNickname).orElse("파티원");
		} catch (Exception e) {
			log.warn("사용자 조회 실패: userId={}", userId);
			return "파티원";
		}
	}

	private String getProductName(Integer productId) {
		if (productId == null)
			return "OTT 서비스";

		try {
			Product product = productDao.getProduct(productId);
			return (product != null && product.getProductName() != null) ? product.getProductName() : "OTT 서비스";
		} catch (Exception e) {
			log.warn("상품 조회 실패: productId={}", productId);
			return "OTT 서비스";
		}
	}

	// ========== 파티 가입 푸시 ==========

	private void sendPartyJoinPush(String userId, String nickname, Party party) {
		TemplatePushRequest pushRequest = TemplatePushRequest.builder().receiverId(userId)
				.pushCode(PushCodeType.PARTY_JOIN.getCode())
				.params(Map.of("nickname", nickname, "productName", getProductName(party.getProductId()),
						"currentCount", String.valueOf(party.getCurrentMembers()), "maxCount",
						String.valueOf(party.getMaxMembers())))
				.moduleId(String.valueOf(party.getPartyId())).moduleType(PushCodeType.PARTY_JOIN.getModuleType())
				.build();

		pushService.addTemplatePush(pushRequest);
		log.info("푸시알림 발송 완료: PARTY_JOIN -> userId={}", userId);
	}

	private void sendPartyMemberJoinPushToLeader(String newMemberUserId, Party party) {
		String nickname = getUserNickname(newMemberUserId);
		String productName = getProductName(party.getProductId());

		TemplatePushRequest pushRequest = TemplatePushRequest.builder().receiverId(party.getPartyLeaderId())
				.pushCode(PushCodeType.PARTY_MEMBER_JOIN.getCode())
				.params(Map.of("nickname", nickname, "productName", productName, "currentCount",
						String.valueOf(party.getCurrentMembers()), "maxCount", String.valueOf(party.getMaxMembers())))
				.moduleId(String.valueOf(party.getPartyId())).moduleType(PushCodeType.PARTY_MEMBER_JOIN.getModuleType())
				.build();

		pushService.addTemplatePush(pushRequest);
		log.info("푸시알림 발송 완료: PARTY_MEMBER_JOIN -> leaderId={}", party.getPartyLeaderId());
	}

	// ========== 파티 시작 푸시 ==========

	private void sendPartyStartPushToAllMembers(Integer partyId, Party party) {
		List<PartyMemberResponse> members = partyMemberDao.findMembersByPartyId(partyId);
		String productName = getProductName(party.getProductId());

		for (PartyMemberResponse member : members) {
			TemplatePushRequest pushRequest = TemplatePushRequest.builder().receiverId(member.getUserId())
					.pushCode(PushCodeType.PARTY_START.getCode()).params(Map.of("productName", productName))
					.moduleId(String.valueOf(partyId)).moduleType(PushCodeType.PARTY_START.getModuleType()).build();

			pushService.addTemplatePush(pushRequest);
		}
		log.info("푸시알림 발송 완료: PARTY_START -> partyId={}, 멤버 {}명", partyId, members.size());
	}

	// ========== 파티 탈퇴 푸시 ==========

	private void sendPartyWithdrawPush(String userId, Party party) {
		String nickname = getUserNickname(userId);
		String productName = getProductName(party.getProductId());

		TemplatePushRequest pushRequest = TemplatePushRequest.builder().receiverId(userId)
				.pushCode(PushCodeType.PARTY_WITHDRAW.getCode())
				.params(Map.of("nickname", nickname, "productName", productName))
				.moduleId(String.valueOf(party.getPartyId())).moduleType(PushCodeType.PARTY_WITHDRAW.getModuleType())
				.build();

		pushService.addTemplatePush(pushRequest);
		log.info("푸시알림 발송 완료: PARTY_WITHDRAW -> userId={}", userId);
	}

	private void sendPartyMemberWithdrawPushToLeader(String withdrawUserId, Party party) {
		String nickname = getUserNickname(withdrawUserId);
		String productName = getProductName(party.getProductId());

		TemplatePushRequest pushRequest = TemplatePushRequest.builder().receiverId(party.getPartyLeaderId())
				.pushCode(PushCodeType.PARTY_MEMBER_WITHDRAW.getCode())
				.params(Map.of("nickname", nickname, "productName", productName))
				.moduleId(String.valueOf(party.getPartyId()))
				.moduleType(PushCodeType.PARTY_MEMBER_WITHDRAW.getModuleType()).build();

		pushService.addTemplatePush(pushRequest);
		log.info("푸시알림 발송 완료: PARTY_MEMBER_WITHDRAW -> leaderId={}", party.getPartyLeaderId());
	}

	// ========== 푸시알림 추가 끝 ==========

	@Override
	public void closeParty(Integer partyId, String reason) {
		log.info("파티 종료 처리 (Scheduler) - partyId: {}, reason: {}", partyId, reason);
		partyDao.updatePartyStatus(partyId, PartyStatus.CLOSED);

	}

	@Override
	public void cancelExpiredParty(Integer partyId, String reason) {
		log.info("만료된 파티 취소 처리 (Scheduler) - partyId: {}, reason: {}", partyId, reason);

		partyDao.updatePartyStatus(partyId, PartyStatus.CLOSED);

	}
}