package com.moa.service.payment.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.moa.common.event.MonthlyPaymentCompletedEvent;
import com.moa.common.event.MonthlyPaymentFailedEvent;
import com.moa.common.exception.BusinessException;
import com.moa.common.exception.ErrorCode;
import com.moa.dao.party.PartyDao;
import com.moa.dao.partymember.PartyMemberDao;
import com.moa.dao.payment.PaymentDao;
import com.moa.dao.product.ProductDao;
import com.moa.dao.user.UserCardDao;
import com.moa.dao.user.UserDao;
import com.moa.domain.Party;
import com.moa.domain.Payment;
import com.moa.domain.Product;
import com.moa.domain.User;
import com.moa.domain.UserCard;
import com.moa.domain.enums.PartyStatus;
import com.moa.domain.enums.PaymentStatus;
import com.moa.domain.enums.PushCodeType;
import com.moa.dto.payment.request.PaymentRequest;
import com.moa.dto.payment.response.PaymentDetailResponse;
import com.moa.dto.payment.response.PaymentResponse;
import com.moa.dto.push.request.TemplatePushRequest;
import com.moa.service.payment.PaymentRetryService;
import com.moa.service.payment.PaymentService;
import com.moa.service.payment.TossPaymentService;
import com.moa.service.push.PushService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

	private final PaymentDao paymentDao;
	private final PartyDao partyDao;
	private final PartyMemberDao partyMemberDao;
	private final TossPaymentService tossPaymentService;
	private final UserCardDao userCardDao;
	private final PaymentRetryService retryService;
	private final ApplicationEventPublisher eventPublisher;

	private final PushService pushService;
	private final ProductDao productDao;
	private final UserDao userDao;

	private static final int MAX_RETRY_ATTEMPTS = 4;

	@Override
	public Payment createInitialPayment(Integer partyId, Integer partyMemberId, String userId, Integer amount,
			String targetMonth, PaymentRequest request) {

		if (isDuplicatePayment(partyMemberId, targetMonth)) {
			throw new BusinessException(ErrorCode.DUPLICATE_PAYMENT);
		}

		if (amount <= 0) {
			throw new BusinessException(ErrorCode.INVALID_PAYMENT_AMOUNT);
		}

		if (paymentDao.findByOrderId(request.getOrderId()).isPresent()) {
			return paymentDao.findByOrderId(request.getOrderId()).get();
		}

		tossPaymentService.confirmPayment(request.getTossPaymentKey(), request.getOrderId(), amount);

		Payment payment = Payment.builder().partyId(partyId).partyMemberId(partyMemberId).userId(userId)
				.paymentType("INITIAL").paymentAmount(amount).paymentStatus(PaymentStatus.COMPLETED)
				.paymentMethod(request.getPaymentMethod()).paymentDate(LocalDateTime.now())
				.tossPaymentKey(request.getTossPaymentKey()).orderId(request.getOrderId()).targetMonth(targetMonth)
				.cardNumber("UNAVAILABLE").cardCompany("TOSS").build();

		paymentDao.insertPayment(payment);

		return payment;
	}

	@Override
	public Payment createMonthlyPayment(Integer partyId, Integer partyMemberId, String userId, Integer amount,
			String targetMonth) {

		if (isDuplicatePayment(partyMemberId, targetMonth)) {
			throw new BusinessException(ErrorCode.DUPLICATE_PAYMENT);
		}

		UserCard userCard = userCardDao.findByUserId(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.BILLING_KEY_NOT_FOUND));

		String orderId = "MONTHLY_" + partyId + "_" + partyMemberId + "_" + System.currentTimeMillis();

		String paymentKey = tossPaymentService.payWithBillingKey(userCard.getBillingKey(), orderId, amount,
				"MOA 월 구독료 (" + targetMonth + ")", userId);

		Payment payment = Payment.builder().partyId(partyId).partyMemberId(partyMemberId).userId(userId)
				.paymentType("MONTHLY").paymentAmount(amount).paymentStatus(PaymentStatus.COMPLETED)
				.paymentMethod("CARD").paymentDate(LocalDateTime.now()).targetMonth(targetMonth)
				.tossPaymentKey(paymentKey).orderId(orderId).cardNumber(userCard.getCardNumber())
				.cardCompany(userCard.getCardCompany()).build();

		paymentDao.insertPayment(payment);

		return payment;
	}

	@Override
	public Payment createDepositPayment(Integer partyId, Integer partyMemberId, String userId, Integer amount,
			String targetMonth, PaymentRequest request) {

		Payment payment = Payment.builder().partyId(partyId).partyMemberId(partyMemberId).userId(userId)
				.paymentType("DEPOSIT").paymentAmount(amount).paymentStatus(PaymentStatus.COMPLETED)
				.paymentMethod(request.getPaymentMethod()).paymentDate(LocalDateTime.now())
				.tossPaymentKey(request.getTossPaymentKey()).orderId(request.getOrderId()).targetMonth(targetMonth)
				.cardNumber("UNAVAILABLE").cardCompany("TOSS").build();

		paymentDao.insertPayment(payment);

		return payment;
	}

	@Override
	public Payment createInitialPaymentWithoutConfirm(Integer partyId, Integer partyMemberId, String userId,
			Integer amount, String targetMonth, PaymentRequest request) {

		if (isDuplicatePayment(partyMemberId, targetMonth)) {
			throw new BusinessException(ErrorCode.DUPLICATE_PAYMENT);
		}

		if (amount <= 0) {
			throw new BusinessException(ErrorCode.INVALID_PAYMENT_AMOUNT);
		}

		Payment payment = Payment.builder().partyId(partyId).partyMemberId(partyMemberId).userId(userId)
				.paymentType("INITIAL").paymentAmount(amount).paymentStatus(PaymentStatus.COMPLETED)
				.paymentMethod(request.getPaymentMethod()).paymentDate(LocalDateTime.now())
				.tossPaymentKey(request.getTossPaymentKey()).orderId(request.getOrderId()).targetMonth(targetMonth)
				.cardNumber("UNAVAILABLE").cardCompany("TOSS").build();

		paymentDao.insertPayment(payment);

		return payment;
	}

	@Override
	@Transactional(readOnly = true)
	public PaymentDetailResponse getPaymentDetail(Integer paymentId) {
		return paymentDao.findDetailById(paymentId)
				.orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
	}

	@Override
	@Transactional(readOnly = true)
	public List<PaymentResponse> getMyPayments(String userId) {
		return paymentDao.findByUserId(userId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<PaymentResponse> getPartyPayments(Integer partyId) {
		return paymentDao.findByPartyId(partyId);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean isDuplicatePayment(Integer partyMemberId, String targetMonth) {
		return paymentDao.findByPartyMemberIdAndTargetMonth(partyMemberId, targetMonth).isPresent();
	}

	@Override
	@Transactional
	public void processMonthlyPayment(Integer partyId, Integer partyMemberId, String userId, Integer amount,
			String targetMonth) {

		if (isDuplicatePayment(partyMemberId, targetMonth)) {
			return;
		}

		Payment payment = Payment.builder().partyId(partyId).partyMemberId(partyMemberId).userId(userId)
				.paymentType("MONTHLY").paymentAmount(amount).paymentStatus(PaymentStatus.PENDING).paymentMethod("CARD")
				.paymentDate(LocalDateTime.now()).targetMonth(targetMonth)
				.orderId("MONTHLY_" + partyId + "_" + partyMemberId + "_" + System.currentTimeMillis()).build();

		paymentDao.insertPayment(payment);
		attemptPaymentExecution(payment, 1);
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void attemptPaymentExecution(Payment payment, int attemptNumber) {
		try {
			UserCard userCard = userCardDao.findByUserId(payment.getUserId())
					.orElseThrow(() -> new BusinessException(ErrorCode.BILLING_KEY_NOT_FOUND));
			String paymentKey = tossPaymentService.payWithBillingKey(userCard.getBillingKey(), payment.getOrderId(),
					payment.getPaymentAmount(), "MOA 월 구독료 (" + payment.getTargetMonth() + ")", payment.getUserId());
			payment.setPaymentStatus(PaymentStatus.COMPLETED);
			payment.setTossPaymentKey(paymentKey);
			payment.setCardNumber(userCard.getCardNumber());
			payment.setCardCompany(userCard.getCardCompany());
			paymentDao.updatePaymentStatus(payment.getPaymentId(), "COMPLETED");
			retryService.recordSuccess(payment, attemptNumber);
			eventPublisher
					.publishEvent(new MonthlyPaymentCompletedEvent(payment.getPartyId(), payment.getPartyMemberId(),
							payment.getUserId(), payment.getPaymentAmount(), payment.getTargetMonth()));

			sendPaymentSuccessPush(payment, attemptNumber);

		} catch (BusinessException e) {
			handlePaymentFailure(payment, attemptNumber, e);
		}
	}

	private void handlePaymentFailure(Payment payment, int attemptNumber, BusinessException e) {
		paymentDao.updatePaymentStatus(payment.getPaymentId(), "FAILED");
		String errorCode = e.getErrorCode().getCode();
		String errorMessage = e.getMessage();

		if (e instanceof com.moa.common.exception.TossPaymentException pe) {
			errorCode = pe.getTossErrorCode();
			errorMessage = pe.getMessage();
		}
		boolean shouldRetry = attemptNumber < MAX_RETRY_ATTEMPTS;

		if (shouldRetry) {
			LocalDateTime nextRetry = calculateNextRetryTime(attemptNumber);
			retryService.recordFailureWithRetry(payment, attemptNumber, errorCode, errorMessage, nextRetry);

			sendPaymentFailedRetryPush(payment, attemptNumber, e.getErrorCode().getCode(), e.getMessage(), nextRetry);

		} else {
			retryService.recordPermanentFailure(payment, attemptNumber, e);
			eventPublisher.publishEvent(new MonthlyPaymentFailedEvent(payment.getPartyId(), payment.getPartyMemberId(),
					payment.getUserId(), payment.getTargetMonth(), e.getMessage()));
			sendPaymentFinalFailedPush(payment, attemptNumber, e.getMessage());
			suspendPartyOnPaymentFailure(payment);
		}
	}

	private void suspendPartyOnPaymentFailure(Payment payment) {
		try {
			Party party = partyDao.findById(payment.getPartyId()).orElse(null);
			if (party == null) {
				log.warn("파티를 찾을 수 없음: partyId={}", payment.getPartyId());
				return;
			}

			if (party.getPartyStatus() == PartyStatus.SUSPENDED || party.getPartyStatus() == PartyStatus.CLOSED) {
				log.info("이미 정지/종료된 파티: partyId={}, status={}", payment.getPartyId(), party.getPartyStatus());
				return;
			}

			partyDao.updatePartyStatus(payment.getPartyId(), PartyStatus.SUSPENDED);
			log.warn("파티 일시정지: partyId={}, 사유=4회 결제 실패", payment.getPartyId());

			sendPartySuspendedPushToLeader(party, payment);
			sendPartySuspendedPushToMember(party, payment);

		} catch (Exception ex) {
			log.error("파티 일시정지 처리 실패: partyId={}, error={}", payment.getPartyId(), ex.getMessage());
		}
	}

	private void sendPartySuspendedPushToLeader(Party party, Payment payment) {
		try {
			String productName = getProductName(party.getProductId());
			String memberNickname = getUserNickname(payment.getUserId());

			Map<String, String> params = Map.of("productName", productName, "memberNickname", memberNickname, "reason",
					"파티원 결제 4회 연속 실패");

			TemplatePushRequest pushRequest = TemplatePushRequest.builder().receiverId(party.getPartyLeaderId())
					.pushCode(PushCodeType.PARTY_SUSPENDED.getCode()).params(params)
					.moduleId(String.valueOf(party.getPartyId()))
					.moduleType(PushCodeType.PARTY_SUSPENDED.getModuleType()).build();

			pushService.addTemplatePush(pushRequest);
			log.info("파티 일시정지 알림 발송: leaderId={}", party.getPartyLeaderId());
		} catch (Exception e) {
			log.error("푸시 발송 실패: {}", e.getMessage());
		}
	}

	private void sendPartySuspendedPushToMember(Party party, Payment payment) {
		try {
			String productName = getProductName(party.getProductId());

			Map<String, String> params = Map.of("productName", productName, "reason",
					"결제 4회 연속 실패로 파티가 일시정지되었습니다. 결제 수단을 확인해주세요.");

			TemplatePushRequest pushRequest = TemplatePushRequest.builder().receiverId(payment.getUserId())
					.pushCode(PushCodeType.PARTY_SUSPENDED.getCode()).params(params)
					.moduleId(String.valueOf(party.getPartyId()))
					.moduleType(PushCodeType.PARTY_SUSPENDED.getModuleType()).build();

			pushService.addTemplatePush(pushRequest);
			log.info("파티 일시정지 알림 발송: memberId={}", payment.getUserId());
		} catch (Exception e) {
			log.error("푸시 발송 실패: {}", e.getMessage());
		}
	}

	@Override
	public void refundPayment(Integer partyId, Integer partyMemberId, String reason) {
		Payment payment = paymentDao.findLastMonthlyPayment(partyId, partyMemberId)
				.orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

		if (!"COMPLETED".equals(payment.getPaymentStatus())) {
			throw new BusinessException(ErrorCode.INVALID_PAYMENT_STATUS);
		}

		try {
			tossPaymentService.cancelPayment(payment.getTossPaymentKey(), reason, null);
			paymentDao.updatePaymentStatus(payment.getPaymentId(), "REFUNDED");
		} catch (com.moa.common.exception.TossPaymentException e) {
			log.error("Toss refund failed: code={}, message={}", e.getTossErrorCode(), e.getMessage());
			throw new BusinessException(ErrorCode.PAYMENT_FAILED, e.getMessage());
		} catch (Exception e) {
			log.error("Refund failed", e);
			throw new BusinessException(ErrorCode.PAYMENT_FAILED);
		}
	}

	private LocalDateTime calculateNextRetryTime(int attemptNumber) {
		int hoursToAdd = 24 * attemptNumber;
		return LocalDateTime.now().plusHours(hoursToAdd);
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

	private void sendPaymentSuccessPush(Payment payment, int attemptNumber) {
		try {
			Party party = partyDao.findById(payment.getPartyId()).orElse(null);
			if (party == null)
				return;

			String productName = getProductName(party.getProductId());
			String pushCode;
			Map<String, String> params;

			if (attemptNumber > 1) {
				pushCode = PushCodeType.PAY_RETRY_SUCCESS.getCode();
				params = Map.of("productName", productName, "attemptNumber", String.valueOf(attemptNumber), "amount",
						String.valueOf(payment.getPaymentAmount()));
			} else {
				pushCode = PushCodeType.PAY_SUCCESS.getCode();
				params = Map.of("productName", productName, "targetMonth", payment.getTargetMonth(), "amount",
						String.valueOf(payment.getPaymentAmount()));
			}

			TemplatePushRequest pushRequest = TemplatePushRequest.builder().receiverId(payment.getUserId())
					.pushCode(pushCode).params(params).moduleId(String.valueOf(payment.getPaymentId()))
					.moduleType(PushCodeType.PAY_SUCCESS.getModuleType()).build();

			pushService.addTemplatePush(pushRequest);
			log.info("푸시알림 발송 완료: {} -> userId={}", pushCode, payment.getUserId());

		} catch (Exception e) {
			log.error("푸시알림 발송 실패: paymentId={}, error={}", payment.getPaymentId(), e.getMessage());
		}
	}

	private void sendPaymentFailedRetryPush(Payment payment, int attemptNumber, String errorCode, String errorMessage,
			LocalDateTime nextRetryDate) {
		try {
			Party party = partyDao.findById(payment.getPartyId()).orElse(null);
			if (party == null)
				return;

			String productName = getProductName(party.getProductId());

			String pushCode = determinePushCodeByError(errorCode);

			Map<String, String> params = Map.of("productName", productName, "attemptNumber",
					String.valueOf(attemptNumber), "errorMessage",
					errorMessage != null ? errorMessage : "결제 처리 중 오류가 발생했습니다.", "nextRetryDate",
					nextRetryDate.toLocalDate().toString());

			TemplatePushRequest pushRequest = TemplatePushRequest.builder().receiverId(payment.getUserId())
					.pushCode(pushCode).params(params).moduleId(String.valueOf(payment.getPaymentId()))
					.moduleType(PushCodeType.PAY_FAILED_RETRY.getModuleType()).build();

			pushService.addTemplatePush(pushRequest);
			log.info("푸시알림 발송 완료: {} -> userId={}", pushCode, payment.getUserId());

		} catch (Exception e) {
			log.error("푸시알림 발송 실패: paymentId={}, error={}", payment.getPaymentId(), e.getMessage());
		}
	}

	private void sendPaymentFinalFailedPush(Payment payment, int attemptNumber, String errorMessage) {
		try {
			Party party = partyDao.findById(payment.getPartyId()).orElse(null);
			if (party == null)
				return;

			String productName = getProductName(party.getProductId());

			Map<String, String> memberParams = Map.of("productName", productName, "attemptNumber",
					String.valueOf(attemptNumber), "errorMessage",
					errorMessage != null ? errorMessage : "결제 처리 중 오류가 발생했습니다.");

			TemplatePushRequest memberPush = TemplatePushRequest.builder().receiverId(payment.getUserId())
					.pushCode(PushCodeType.PAY_FINAL_FAILED.getCode()).params(memberParams)
					.moduleId(String.valueOf(payment.getPaymentId()))
					.moduleType(PushCodeType.PAY_FINAL_FAILED.getModuleType()).build();

			pushService.addTemplatePush(memberPush);
			log.info("푸시알림 발송 완료: PAY_FINAL_FAILED -> userId={}", payment.getUserId());
			String memberNickname = getUserNickname(payment.getUserId());

			Map<String, String> leaderParams = Map.of("memberNickname", memberNickname, "productName", productName,
					"errorMessage", errorMessage != null ? errorMessage : "결제 처리 중 오류가 발생했습니다.");

			TemplatePushRequest leaderPush = TemplatePushRequest.builder().receiverId(party.getPartyLeaderId())
					.pushCode(PushCodeType.PAY_MEMBER_FAILED_LEADER.getCode()).params(leaderParams)
					.moduleId(String.valueOf(payment.getPaymentId()))
					.moduleType(PushCodeType.PAY_MEMBER_FAILED_LEADER.getModuleType()).build();

			pushService.addTemplatePush(leaderPush);
			log.info("푸시알림 발송 완료: PAY_MEMBER_FAILED_LEADER -> leaderId={}", party.getPartyLeaderId());

		} catch (Exception e) {
			log.error("푸시알림 발송 실패: paymentId={}, error={}", payment.getPaymentId(), e.getMessage());
		}
	}

	private String determinePushCodeByError(String errorCode) {
		if (errorCode == null) {
			return PushCodeType.PAY_FAILED_RETRY.getCode();
		}

		return switch (errorCode) {
		case "INSUFFICIENT_BALANCE", "NOT_ENOUGH_BALANCE" -> PushCodeType.PAY_FAILED_BALANCE.getCode();

		case "EXCEED_CARD_LIMIT", "DAILY_LIMIT_EXCEEDED", "MONTHLY_LIMIT_EXCEEDED" ->
			PushCodeType.PAY_FAILED_LIMIT.getCode();

		case "INVALID_CARD_NUMBER", "INVALID_CARD_EXPIRATION", "INVALID_CVV", "CARD_EXPIRED", "CARD_RESTRICTED",
				"CARD_LOST_OR_STOLEN" ->
			PushCodeType.PAY_FAILED_CARD.getCode();

		default -> PushCodeType.PAY_FAILED_RETRY.getCode();
		};
	}
}