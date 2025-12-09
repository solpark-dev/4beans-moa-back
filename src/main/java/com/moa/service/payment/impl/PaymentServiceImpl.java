package com.moa.service.payment.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.moa.common.exception.BusinessException;
import com.moa.common.exception.ErrorCode;
import com.moa.dao.partymember.PartyMemberDao;
import com.moa.dao.party.PartyDao;
import com.moa.dao.payment.PaymentDao;
import com.moa.dao.user.UserCardDao;
import com.moa.domain.Party;
import com.moa.domain.PartyMember;
import com.moa.domain.Payment;
import com.moa.domain.UserCard;
import com.moa.domain.enums.MemberStatus;
import com.moa.domain.enums.PartyStatus;
import com.moa.domain.enums.PaymentStatus;
import com.moa.dto.payment.request.PaymentRequest;
import com.moa.dto.payment.response.PaymentDetailResponse;
import com.moa.dto.payment.response.PaymentResponse;
import com.moa.common.event.MonthlyPaymentCompletedEvent;
import com.moa.common.event.MonthlyPaymentFailedEvent;
import com.moa.service.payment.PaymentRetryService;
import com.moa.service.payment.PaymentService;
import com.moa.service.payment.TossPaymentService;

import lombok.RequiredArgsConstructor;

/**
 * 결제 서비스 구현체
 *
 * v1.0 가정:
 * - 모든 결제는 즉시 성공 (Happy Path)
 * - Toss Payments API는 항상 성공
 * - 결제 실패 케이스 없음
 */
@Service
@Transactional
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

        private final PaymentDao paymentDao;
        private final PartyDao partyDao;
        private final PartyMemberDao partyMemberDao;
        private final TossPaymentService tossPaymentService;
        private final UserCardDao userCardDao;
        private final PaymentRetryService retryService;
        private final ApplicationEventPublisher eventPublisher;

        private static final int MAX_RETRY_ATTEMPTS = 4;

        @Override
        public Payment createInitialPayment(
                        Integer partyId,
                        Integer partyMemberId,
                        String userId,
                        Integer amount,
                        String targetMonth,
                        PaymentRequest request) {

                // 1. 중복 결제 확인
                if (isDuplicatePayment(partyMemberId, targetMonth)) {
                        throw new BusinessException(ErrorCode.DUPLICATE_PAYMENT);
                }

                // 2. 결제 금액 검증
                if (amount <= 0) {
                        throw new BusinessException(ErrorCode.INVALID_PAYMENT_AMOUNT);
                }

                // 3. orderId 중복 확인 (토스 API 중복 호출 방지)
                if (paymentDao.findByOrderId(request.getOrderId()).isPresent()) {
                        // 이미 처리된 orderId면 기존 결제 정보 반환
                        return paymentDao.findByOrderId(request.getOrderId()).get();
                }

                // 4. Toss Payments 결제 승인
                tossPaymentService.confirmPayment(
                                request.getTossPaymentKey(),
                                request.getOrderId(),
                                amount);

                // 5. Payment 엔티티 생성
                Payment payment = Payment.builder()
                                .partyId(partyId)
                                .partyMemberId(partyMemberId)
                                .userId(userId)
                                .paymentType("INITIAL") // 첫 달 결제
                                .paymentAmount(amount)
                                .paymentStatus(PaymentStatus.COMPLETED) // v1.0: 즉시 완료
                                .paymentMethod(request.getPaymentMethod())
                                .paymentDate(LocalDateTime.now())
                                .tossPaymentKey(request.getTossPaymentKey())
                                .orderId(request.getOrderId())
                                .targetMonth(targetMonth)
                                .cardNumber("UNAVAILABLE")
                                .cardCompany("TOSS")
                                .build();

                // 6. DB 저장
                paymentDao.insertPayment(payment);

                return payment;
        }

        @Override
        public Payment createMonthlyPayment(
                        Integer partyId,
                        Integer partyMemberId,
                        String userId,
                        Integer amount,
                        String targetMonth) {

                // 1. 중복 결제 확인
                if (isDuplicatePayment(partyMemberId, targetMonth)) {
                        throw new BusinessException(ErrorCode.DUPLICATE_PAYMENT);
                }

                // 2. 빌링키 조회
                UserCard userCard = userCardDao.findByUserId(userId)
                                .orElseThrow(() -> new BusinessException(ErrorCode.BILLING_KEY_NOT_FOUND));

                // 3. 주문 ID 생성
                String orderId = "MONTHLY_" + partyId + "_" + partyMemberId + "_" + System.currentTimeMillis();

                // 4. Toss Payments 빌링키 결제 요청
                String paymentKey = tossPaymentService.payWithBillingKey(
                                userCard.getBillingKey(),
                                orderId,
                                amount,
                                "MOA 월 구독료 (" + targetMonth + ")");

                // 5. Payment 엔티티 생성
                Payment payment = Payment.builder()
                                .partyId(partyId)
                                .partyMemberId(partyMemberId)
                                .userId(userId)
                                .paymentType("MONTHLY") // 월별 자동 결제
                                .paymentAmount(amount)
                                .paymentStatus(PaymentStatus.COMPLETED)
                                .paymentMethod("CARD") // 빌링키 결제도 카드로 간주
                                .paymentDate(LocalDateTime.now())
                                .targetMonth(targetMonth)
                                .tossPaymentKey(paymentKey) // 결제 키 저장
                                .orderId(orderId) // 주문 ID 저장
                                .cardNumber(userCard.getCardNumber()) // 카드 번호 저장
                                .cardCompany(userCard.getCardCompany()) // 카드사 저장
                                .build();

                // 6. DB 저장
                paymentDao.insertPayment(payment);

                return payment;
        }

        @Override
        public Payment createDepositPayment(
                        Integer partyId,
                        Integer partyMemberId,
                        String userId,
                        Integer amount,
                        String targetMonth,
                        PaymentRequest request) {

                // 1. Payment 엔티티 생성
                Payment payment = Payment.builder()
                                .partyId(partyId)
                                .partyMemberId(partyMemberId)
                                .userId(userId)
                                .paymentType("DEPOSIT") // 보증금
                                .paymentAmount(amount)
                                .paymentStatus(PaymentStatus.COMPLETED) // v1.0: 즉시 완료
                                .paymentMethod(request.getPaymentMethod())
                                .paymentDate(LocalDateTime.now())
                                .tossPaymentKey(request.getTossPaymentKey())
                                .orderId(request.getOrderId())
                                .targetMonth(targetMonth)
                                .cardNumber("UNAVAILABLE")
                                .cardCompany("TOSS")
                                .build();

                // 2. DB 저장
                paymentDao.insertPayment(payment);

                return payment;
        }

        @Override
        public Payment createInitialPaymentWithoutConfirm(
                        Integer partyId,
                        Integer partyMemberId,
                        String userId,
                        Integer amount,
                        String targetMonth,
                        PaymentRequest request) {

                // 1. 중복 결제 확인
                if (isDuplicatePayment(partyMemberId, targetMonth)) {
                        throw new BusinessException(ErrorCode.DUPLICATE_PAYMENT);
                }

                // 2. 결제 금액 검증
                if (amount <= 0) {
                        throw new BusinessException(ErrorCode.INVALID_PAYMENT_AMOUNT);
                }

                // 3. Toss 승인은 이미 createDeposit에서 완료됨 - 생략

                // 4. Payment 엔티티 생성
                Payment payment = Payment.builder()
                                .partyId(partyId)
                                .partyMemberId(partyMemberId)
                                .userId(userId)
                                .paymentType("INITIAL") // 첫 달 결제
                                .paymentAmount(amount)
                                .paymentStatus(PaymentStatus.COMPLETED) // v1.0: 즉시 완료
                                .paymentMethod(request.getPaymentMethod())
                                .paymentDate(LocalDateTime.now())
                                .tossPaymentKey(request.getTossPaymentKey())
                                .orderId(request.getOrderId())
                                .targetMonth(targetMonth)
                                .cardNumber("UNAVAILABLE")
                                .cardCompany("TOSS")
                                .build();

                // 5. DB 저장
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
                return paymentDao.findByPartyMemberIdAndTargetMonth(partyMemberId, targetMonth)
                                .isPresent();
        }

        // ============================================
        // New Methods for Monthly Auto-Payment with Retry Logic
        // ============================================

        @Override
        @Transactional
        public void processMonthlyPayment(
                        Integer partyId,
                        Integer partyMemberId,
                        String userId,
                        Integer amount,
                        String targetMonth) {

                // 1. Check for duplicate payment
                if (isDuplicatePayment(partyMemberId, targetMonth)) {
                        return;
                }

                // 2. Create PENDING payment record
                Payment payment = Payment.builder()
                                .partyId(partyId)
                                .partyMemberId(partyMemberId)
                                .userId(userId)
                                .paymentType("MONTHLY")
                                .paymentAmount(amount)
                                .paymentStatus(PaymentStatus.PENDING) // Start as PENDING
                                .paymentMethod("CARD")
                                .paymentDate(LocalDateTime.now())
                                .targetMonth(targetMonth)
                                .orderId("MONTHLY_" + partyId + "_" + partyMemberId + "_" + System.currentTimeMillis())
                                .build();

                paymentDao.insertPayment(payment);

                // 3. Attempt execution (separate transaction)
                attemptPaymentExecution(payment, 1);
        }

        @Override
        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void attemptPaymentExecution(Payment payment, int attemptNumber) {
                try {
                        // 1. Get billing key
                        UserCard userCard = userCardDao.findByUserId(payment.getUserId())
                                        .orElseThrow(() -> new BusinessException(ErrorCode.BILLING_KEY_NOT_FOUND));

                        // 2. Call Toss API with billing key
                        String paymentKey = tossPaymentService.payWithBillingKey(
                                        userCard.getBillingKey(),
                                        payment.getOrderId(),
                                        payment.getPaymentAmount(),
                                        "MOA 월 구독료 (" + payment.getTargetMonth() + ")");

                        // 3. Update payment to COMPLETED
                        payment.setPaymentStatus(PaymentStatus.COMPLETED);
                        payment.setTossPaymentKey(paymentKey);
                        payment.setCardNumber(userCard.getCardNumber());
                        payment.setCardCompany(userCard.getCardCompany());
                        paymentDao.updatePaymentStatus(payment.getPaymentId(), "COMPLETED");

                        // 4. Record success in retry history
                        retryService.recordSuccess(payment, attemptNumber);

                        // 5. Publish success event
                        eventPublisher.publishEvent(new MonthlyPaymentCompletedEvent(
                                        payment.getPartyId(),
                                        payment.getPartyMemberId(),
                                        payment.getUserId(),
                                        payment.getPaymentAmount(),
                                        payment.getTargetMonth()));

                } catch (BusinessException e) {
                        // Payment failed - handle failure and schedule retry
                        handlePaymentFailure(payment, attemptNumber, e);
                }
        }

        private void handlePaymentFailure(Payment payment, int attemptNumber, BusinessException e) {
                // 1. Update payment status to FAILED
                paymentDao.updatePaymentStatus(payment.getPaymentId(), "FAILED");

                // 2. Determine if retry should be scheduled
                boolean shouldRetry = attemptNumber < MAX_RETRY_ATTEMPTS;

                if (shouldRetry) {
                        // Schedule retry with exponential backoff
                        LocalDateTime nextRetry = calculateNextRetryTime(attemptNumber);
                        retryService.recordFailureWithRetry(
                                        payment,
                                        attemptNumber,
                                        e.getErrorCode().getCode(),
                                        e.getMessage(),
                                        nextRetry);
                } else {
                        // Max retries exceeded - permanent failure
                        retryService.recordPermanentFailure(payment, attemptNumber, e);

                        // Publish failure event for notification
                        eventPublisher.publishEvent(new MonthlyPaymentFailedEvent(
                                        payment.getPartyId(),
                                        payment.getPartyMemberId(),
                                        payment.getUserId(),
                                        payment.getTargetMonth(),
                                        e.getMessage()));
                }
        }

        @Override
        @Transactional
        public void refundPayment(Integer partyId, Integer partyMemberId, String reason) {
                // 1. 초기 결제(INITIAL) 내역 조회
                Payment payment = paymentDao.findByPartyMemberIdAndType(partyMemberId, "INITIAL")
                                .orElse(null);

                if (payment == null) {
                        // 결제 내역이 없으면 무시 (보증금만 낸 경우 등)
                        return;
                }

                // 2. 이미 환불되었는지 확인
                if (payment.getPaymentStatus() == PaymentStatus.REFUNDED) {
                        return;
                }

                // 3. Toss Payments 결제 취소 API 호출
                try {
                        tossPaymentService.cancelPayment(
                                        payment.getTossPaymentKey(),
                                        reason,
                                        payment.getPaymentAmount());
                } catch (Exception e) {
                        // 환불 실패 시 로그 남기고 예외 던짐
                        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "결제 환불 실패: " + e.getMessage());
                }

                // 4. 상태 업데이트
                payment.setPaymentStatus(PaymentStatus.REFUNDED);
                paymentDao.updatePaymentStatus(payment.getPaymentId(), "REFUNDED");
        }

        private LocalDateTime calculateNextRetryTime(int attemptNumber) {
                // Exponential backoff: 24h, 48h, 72h
                int hoursToAdd = 24 * attemptNumber;
                return LocalDateTime.now().plusHours(hoursToAdd);
        }
}