package com.moa.service.party.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moa.common.exception.BusinessException;
import com.moa.common.exception.ErrorCode;
import com.moa.common.util.AESUtil; // Import AESUtil
import com.moa.dao.party.PartyDao;
import com.moa.dao.partymember.PartyMemberDao;
import com.moa.dao.product.ProductDao;
import com.moa.dao.user.UserDao;
import com.moa.domain.Deposit;
import com.moa.domain.Party;
import com.moa.domain.PartyMember;
import com.moa.domain.Product;
import com.moa.domain.User;
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

	public PartyServiceImpl(PartyDao partyDao, PartyMemberDao partyMemberDao, ProductDao productDao,
			DepositService depositService, PaymentService paymentService, PushService pushService,
			com.moa.service.payment.TossPaymentService tossPaymentService,
			com.moa.service.refund.RefundRetryService refundRetryService, UserDao userDao) {
		this.partyDao = partyDao;
		this.partyMemberDao = partyMemberDao;
		this.productDao = productDao;
		this.depositService = depositService;
		this.paymentService = paymentService;
		this.pushService = pushService;
		this.tossPaymentService = tossPaymentService;
		this.refundRetryService = refundRetryService;
		this.userDao = userDao;
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
	public PartyDetailResponse processLeaderDeposit(Integer partyId, String userId, PaymentRequest paymentRequest) {

		Party party = partyDao.findById(partyId).orElseThrow(() -> new BusinessException(ErrorCode.PARTY_NOT_FOUND));

		if (!party.getPartyLeaderId().equals(userId)) {
			throw new BusinessException(ErrorCode.NOT_PARTY_LEADER);
		}

		if (party.getPartyStatus() != PartyStatus.PENDING_PAYMENT) {
			throw new BusinessException(ErrorCode.INVALID_PARTY_STATUS);
		}

		PartyMember leaderMember = partyMemberDao.findByPartyIdAndUserId(partyId, userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.PARTY_MEMBER_NOT_FOUND));
		int depositAmount = party.getMonthlyFee() * party.getMaxMembers();

		depositService.createDeposit(partyId, leaderMember.getPartyMemberId(), userId, depositAmount, paymentRequest);

		String targetMonth = party.getStartDate().format(DateTimeFormatter.ofPattern("yyyy-MM"));
		paymentService.createDepositPayment(partyId, leaderMember.getPartyMemberId(), userId, depositAmount,
				targetMonth, paymentRequest);

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

		int updatedRows = partyDao.incrementCurrentMembers(partyId);
		if (updatedRows == 0) {

			throw new BusinessException(ErrorCode.PARTY_FULL);
		}

		int fee = party.getMonthlyFee();

		PartyMember partyMember = PartyMember.builder().partyId(partyId).userId(userId).memberRole("MEMBER")
				.memberStatus(MemberStatus.PENDING_PAYMENT).joinDate(LocalDateTime.now()).build();
		partyMemberDao.insertPartyMember(partyMember);

		int totalAmount = fee * 2;
		try {
			tossPaymentService.confirmPayment(paymentRequest.getTossPaymentKey(), paymentRequest.getOrderId(),
					totalAmount);
		} catch (Exception e) {

			partyDao.decrementCurrentMembers(partyId);
			partyMemberDao.deletePartyMember(partyMember.getPartyMemberId());
			log.error("Toss 결제 실패, 정원 복구: partyId={}, error={}", partyId, e.getMessage());
			throw e;
		}

		try {
			depositService.createDepositWithoutConfirm(partyId, partyMember.getPartyMemberId(), userId, fee,
					paymentRequest);
		} catch (Exception e) {

			partyDao.decrementCurrentMembers(partyId);
			Deposit pendingDeposit = Deposit.builder().depositId(null).partyId(partyId).userId(userId)
					.depositAmount(fee).tossPaymentKey(paymentRequest.getTossPaymentKey()).build();
			refundRetryService.recordCompensation(pendingDeposit, "Toss 성공 후 Deposit 생성 실패");
			log.error("Deposit 생성 실패, 보상 트랜잭션 등록: partyId={}", partyId);
			throw e;
		}

		String targetMonth = party.getStartDate().format(DateTimeFormatter.ofPattern("yyyy-MM"));
		paymentService.createInitialPaymentWithoutConfirm(partyId, partyMember.getPartyMemberId(), userId, fee,
				targetMonth, paymentRequest);

		partyMember.setMemberStatus(MemberStatus.ACTIVE);
		partyMemberDao.updatePartyMember(partyMember);

		Party updatedParty = partyDao.findById(partyId)
				.orElseThrow(() -> new BusinessException(ErrorCode.PARTY_NOT_FOUND));

		if (updatedParty.getCurrentMembers() >= updatedParty.getMaxMembers()) {
			partyDao.updatePartyStatus(partyId, PartyStatus.ACTIVE);
			safeSendPush(() -> sendPartyStartPushToAllMembers(partyId, updatedParty));
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