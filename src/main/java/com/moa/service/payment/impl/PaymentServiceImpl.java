package com.moa.service.payment.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.moa.common.exception.BusinessException;
import com.moa.common.exception.ErrorCode;
import com.moa.dao.partymember.PartyMemberDao;
import com.moa.dao.party.PartyDao;
import com.moa.dao.payment.PaymentDao;
import com.moa.dao.product.ProductDao;
import com.moa.dao.user.UserCardDao;
import com.moa.dao.user.UserDao;
import com.moa.domain.Party;
import com.moa.domain.PartyMember;
import com.moa.domain.Payment;
import com.moa.domain.Product;
import com.moa.domain.User;
import com.moa.domain.UserCard;
import com.moa.domain.enums.MemberStatus;
import com.moa.domain.enums.PartyStatus;
import com.moa.domain.enums.PaymentStatus;
import com.moa.domain.enums.PushCodeType;
import com.moa.dto.payment.request.PaymentRequest;
import com.moa.dto.payment.response.PaymentDetailResponse;
import com.moa.dto.payment.response.PaymentResponse;
import com.moa.dto.push.request.TemplatePushRequest;
import com.moa.common.event.MonthlyPaymentCompletedEvent;
import com.moa.common.event.MonthlyPaymentFailedEvent;
import com.moa.service.payment.PaymentRetryService;
import com.moa.service.payment.PaymentService;
import com.moa.service.payment.TossPaymentService;
import com.moa.service.push.PushService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
public class PaymentServiceImpl implements PaymentService {

        private final PaymentDao paymentDao;
        private final PartyDao partyDao;
        private final PartyMemberDao partyMemberDao;
        private final TossPaymentService tossPaymentService;
        private final UserCardDao userCardDao;
        private final PaymentRetryService retryService;
        private final ApplicationEventPublisher eventPublisher;

        // ========== 푸시알림 추가 ==========
        private final PushService pushService;
        private final ProductDao productDao;
        private final UserDao userDao;
        // ========== 푸시알림 추가 끝 ==========

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
                                "MOA 월 구독료 (" + targetMonth + ")",
                                userId);

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
                                        "MOA 월 구독료 (" + payment.getTargetMonth() + ")",
                                        payment.getUserId());

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

                        // ========== 푸시알림 추가: 결제 성공 ==========
                        sendPaymentSuccessPush(payment, attemptNumber);
                        // ========== 푸시알림 추가 끝 ==========

                } catch (BusinessException e) {
                        // Payment failed - handle failure and schedule retry
                        handlePaymentFailure(payment, attemptNumber, e);
                }
        }

        private void handlePaymentFailure(Payment payment, int attemptNumber, BusinessException e) {
                // 1. Update payment status to FAILED
                paymentDao.updatePaymentStatus(payment.getPaymentId(), "FAILED");

                // 2. Determine error info
                String errorCode = e.getErrorCode().getCode();
                String errorMessage = e.getMessage();

                if (e instanceof com.moa.common.exception.TossPaymentException pe) {
                        errorCode = pe.getTossErrorCode();
                        errorMessage = pe.getMessage();
                }

                // 3. Determine if retry should be scheduled
                boolean shouldRetry = attemptNumber < MAX_RETRY_ATTEMPTS;

                if (shouldRetry) {
                        // Schedule retry with exponential backoff
                        LocalDateTime nextRetry = calculateNextRetryTime(attemptNumber);
                        retryService.recordFailureWithRetry(
                                        payment,
                                        attemptNumber,
                                        errorCode,
                                        errorMessage,
                                        nextRetry);

                        // ========== 푸시알림 추가: 결제 실패 (재시도 예정) ==========
                        sendPaymentFailedRetryPush(payment, attemptNumber, e.getErrorCode().getCode(), e.getMessage(), nextRetry);
                        // ========== 푸시알림 추가 끝 ==========

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

                        // ========== 푸시알림 추가: 결제 최종 실패 ==========
                        sendPaymentFinalFailedPush(payment, attemptNumber, e.getMessage());
                        // ========== 푸시알림 추가 끝 ==========

                        // ========== 파티 SUSPENDED 처리: 4회 실패 시 파티 일시정지 ==========
                        suspendPartyOnPaymentFailure(payment);
                        // ========== 파티 SUSPENDED 처리 끝 ==========
                }
        }

        /**
         * 4회 결제 실패 시 파티 일시정지 처리
         * 결제 실패가 누적되면 파티를 SUSPENDED 상태로 전환하여
         * 추가 결제 시도를 중단하고 관리자/파티장에게 알림
         */
        private void suspendPartyOnPaymentFailure(Payment payment) {
                try {
                        Party party = partyDao.findById(payment.getPartyId()).orElse(null);
                        if (party == null) {
                                log.warn("파티를 찾을 수 없음: partyId={}", payment.getPartyId());
                                return;
                        }

                        // 이미 SUSPENDED 또는 CLOSED 상태면 무시
                        if (party.getPartyStatus() == PartyStatus.SUSPENDED
                                        || party.getPartyStatus() == PartyStatus.CLOSED) {
                                log.info("이미 정지/종료된 파티: partyId={}, status={}",
                                                payment.getPartyId(), party.getPartyStatus());
                                return;
                        }

                        // 파티 상태를 SUSPENDED로 변경
                        partyDao.updatePartyStatus(payment.getPartyId(), PartyStatus.SUSPENDED);
                        log.warn("파티 일시정지: partyId={}, 사유=4회 결제 실패", payment.getPartyId());

                        // 파티장에게 알림 발송
                        sendPartySuspendedPushToLeader(party, payment);

                        // 해당 파티원에게 알림 발송
                        sendPartySuspendedPushToMember(party, payment);

                } catch (Exception ex) {
                        log.error("파티 일시정지 처리 실패: partyId={}, error={}",
                                        payment.getPartyId(), ex.getMessage());
                }
        }

        /**
         * 파티 일시정지 알림 - 파티장에게
         */
        private void sendPartySuspendedPushToLeader(Party party, Payment payment) {
                try {
                        String productName = getProductName(party.getProductId());
                        String memberNickname = getUserNickname(payment.getUserId());

                        Map<String, String> params = Map.of(
                                "productName", productName,
                                "memberNickname", memberNickname,
                                "reason", "파티원 결제 4회 연속 실패"
                        );

                        TemplatePushRequest pushRequest = TemplatePushRequest.builder()
                                .receiverId(party.getPartyLeaderId())
                                .pushCode(PushCodeType.PARTY_SUSPENDED.getCode())
                                .params(params)
                                .moduleId(String.valueOf(party.getPartyId()))
                                .moduleType(PushCodeType.PARTY_SUSPENDED.getModuleType())
                                .build();

                        pushService.addTemplatePush(pushRequest);
                        log.info("파티 일시정지 알림 발송: leaderId={}", party.getPartyLeaderId());
                } catch (Exception e) {
                        log.error("푸시 발송 실패: {}", e.getMessage());
                }
        }

        /**
         * 파티 일시정지 알림 - 해당 파티원에게
         */
        private void sendPartySuspendedPushToMember(Party party, Payment payment) {
                try {
                        String productName = getProductName(party.getProductId());

                        Map<String, String> params = Map.of(
                                "productName", productName,
                                "reason", "결제 4회 연속 실패로 파티가 일시정지되었습니다. 결제 수단을 확인해주세요."
                        );

                        TemplatePushRequest pushRequest = TemplatePushRequest.builder()
                                .receiverId(payment.getUserId())
                                .pushCode(PushCodeType.PARTY_SUSPENDED.getCode())
                                .params(params)
                                .moduleId(String.valueOf(party.getPartyId()))
                                .moduleType(PushCodeType.PARTY_SUSPENDED.getModuleType())
                                .build();

                        pushService.addTemplatePush(pushRequest);
                        log.info("파티 일시정지 알림 발송: memberId={}", payment.getUserId());
                } catch (Exception e) {
                        log.error("푸시 발송 실패: {}", e.getMessage());
                }
        }

        /**
         * 월회비 환불 (파티 탈퇴, 파티 취소 시)
         */
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
                        // TODO: 환불 재시도 로직 필요
                        throw new BusinessException(ErrorCode.PAYMENT_FAILED, e.getMessage());
                } catch (Exception e) {
                        log.error("Refund failed", e);
                        throw new BusinessException(ErrorCode.PAYMENT_FAILED);
                }
        }

        private LocalDateTime calculateNextRetryTime(int attemptNumber) {
                // Exponential backoff: 24h, 48h, 72h
                int hoursToAdd = 24 * attemptNumber;
                return LocalDateTime.now().plusHours(hoursToAdd);
        }

     // ============================================
     // 푸시알림 추가: Private 메서드들
     // ============================================

     /**
      * 푸시알림 추가: 상품명 조회 헬퍼 메서드
      */
     private String getProductName(Integer productId) {
         if (productId == null) return "OTT 서비스";

         try {
             Product product = productDao.getProduct(productId);
             return (product != null && product.getProductName() != null)
                 ? product.getProductName() : "OTT 서비스";
         } catch (Exception e) {
             log.warn("상품 조회 실패: productId={}", productId);
             return "OTT 서비스";
         }
     }

     /**
      * 푸시알림 추가: 사용자 닉네임 조회 헬퍼 메서드
      */
     private String getUserNickname(String userId) {
         if (userId == null) return "파티원";

         try {
             return userDao.findByUserId(userId)
                 .map(User::getNickname)
                 .orElse("파티원");
         } catch (Exception e) {
             log.warn("사용자 조회 실패: userId={}", userId);
             return "파티원";
         }
     }

     /**
      * 푸시알림 추가: 결제 성공 알림
      */
     private void sendPaymentSuccessPush(Payment payment, int attemptNumber) {
         try {
             Party party = partyDao.findById(payment.getPartyId()).orElse(null);
             if (party == null) return;

             // ========== 수정: productName 조회 ==========
             String productName = getProductName(party.getProductId());
             // ========== 수정 끝 ==========

             String pushCode;
             Map<String, String> params;

             // 재시도 성공인 경우
             if (attemptNumber > 1) {
                 pushCode = PushCodeType.PAY_RETRY_SUCCESS.getCode();
                 params = Map.of(
                     "productName", productName,
                     "attemptNumber", String.valueOf(attemptNumber),
                     "amount", String.valueOf(payment.getPaymentAmount())
                 );
             } else {
                 // 첫 시도 성공
                 pushCode = PushCodeType.PAY_SUCCESS.getCode();
                 params = Map.of(
                     "productName", productName,
                     "targetMonth", payment.getTargetMonth(),
                     "amount", String.valueOf(payment.getPaymentAmount())
                 );
             }

             TemplatePushRequest pushRequest = TemplatePushRequest.builder()
                 .receiverId(payment.getUserId())
                 .pushCode(pushCode)
                 .params(params)
                 .moduleId(String.valueOf(payment.getPaymentId()))
                 .moduleType(PushCodeType.PAY_SUCCESS.getModuleType())
                 .build();

             pushService.addTemplatePush(pushRequest);
             log.info("푸시알림 발송 완료: {} -> userId={}", pushCode, payment.getUserId());

         } catch (Exception e) {
             log.error("푸시알림 발송 실패: paymentId={}, error={}", payment.getPaymentId(), e.getMessage());
         }
     }

     /**
      * 푸시알림 추가: 결제 실패 (재시도 예정) 알림
      */
     private void sendPaymentFailedRetryPush(Payment payment, int attemptNumber, String errorCode, String errorMessage, LocalDateTime nextRetryDate) {
         try {
             Party party = partyDao.findById(payment.getPartyId()).orElse(null);
             if (party == null) return;

             // ========== 수정: productName 조회 ==========
             String productName = getProductName(party.getProductId());
             // ========== 수정 끝 ==========

             // 에러 코드에 따른 푸시 코드 결정
             String pushCode = determinePushCodeByError(errorCode);

             Map<String, String> params = Map.of(
                 "productName", productName,
                 "attemptNumber", String.valueOf(attemptNumber),
                 "errorMessage", errorMessage != null ? errorMessage : "결제 처리 중 오류가 발생했습니다.",
                 "nextRetryDate", nextRetryDate.toLocalDate().toString()
             );

             TemplatePushRequest pushRequest = TemplatePushRequest.builder()
                 .receiverId(payment.getUserId())
                 .pushCode(pushCode)
                 .params(params)
                 .moduleId(String.valueOf(payment.getPaymentId()))
                 .moduleType(PushCodeType.PAY_FAILED_RETRY.getModuleType())
                 .build();

             pushService.addTemplatePush(pushRequest);
             log.info("푸시알림 발송 완료: {} -> userId={}", pushCode, payment.getUserId());

         } catch (Exception e) {
             log.error("푸시알림 발송 실패: paymentId={}, error={}", payment.getPaymentId(), e.getMessage());
         }
     }

     /**
      * 푸시알림 추가: 결제 최종 실패 알림 (파티원 + 방장)
      */
     private void sendPaymentFinalFailedPush(Payment payment, int attemptNumber, String errorMessage) {
         try {
             Party party = partyDao.findById(payment.getPartyId()).orElse(null);
             if (party == null) return;

             // ========== 수정: productName 조회 ==========
             String productName = getProductName(party.getProductId());
             // ========== 수정 끝 ==========

             // 1. 파티원에게 알림
             Map<String, String> memberParams = Map.of(
                 "productName", productName,
                 "attemptNumber", String.valueOf(attemptNumber),
                 "errorMessage", errorMessage != null ? errorMessage : "결제 처리 중 오류가 발생했습니다."
             );

             TemplatePushRequest memberPush = TemplatePushRequest.builder()
                 .receiverId(payment.getUserId())
                 .pushCode(PushCodeType.PAY_FINAL_FAILED.getCode())
                 .params(memberParams)
                 .moduleId(String.valueOf(payment.getPaymentId()))
                 .moduleType(PushCodeType.PAY_FINAL_FAILED.getModuleType())
                 .build();

             pushService.addTemplatePush(memberPush);
             log.info("푸시알림 발송 완료: PAY_FINAL_FAILED -> userId={}", payment.getUserId());

             // ========== 수정: memberNickname 조회 ==========
             // 2. 방장에게 알림 (UserDao로 닉네임 조회)
             String memberNickname = getUserNickname(payment.getUserId());
             // ========== 수정 끝 ==========

             Map<String, String> leaderParams = Map.of(
                 "memberNickname", memberNickname,
                 "productName", productName,
                 "errorMessage", errorMessage != null ? errorMessage : "결제 처리 중 오류가 발생했습니다."
             );

             TemplatePushRequest leaderPush = TemplatePushRequest.builder()
                 .receiverId(party.getPartyLeaderId())
                 .pushCode(PushCodeType.PAY_MEMBER_FAILED_LEADER.getCode())
                 .params(leaderParams)
                 .moduleId(String.valueOf(payment.getPaymentId()))
                 .moduleType(PushCodeType.PAY_MEMBER_FAILED_LEADER.getModuleType())
                 .build();

             pushService.addTemplatePush(leaderPush);
             log.info("푸시알림 발송 완료: PAY_MEMBER_FAILED_LEADER -> leaderId={}", party.getPartyLeaderId());

         } catch (Exception e) {
             log.error("푸시알림 발송 실패: paymentId={}, error={}", payment.getPaymentId(), e.getMessage());
         }
     }

     /**
      * 푸시알림 추가: Toss 에러 코드에 따른 푸시 코드 결정
      */
     private String determinePushCodeByError(String errorCode) {
         if (errorCode == null) {
             return PushCodeType.PAY_FAILED_RETRY.getCode();
         }

         return switch (errorCode) {
             // 잔액 부족
             case "INSUFFICIENT_BALANCE", "NOT_ENOUGH_BALANCE"
                 -> PushCodeType.PAY_FAILED_BALANCE.getCode();

             // 한도 초과
             case "EXCEED_CARD_LIMIT", "DAILY_LIMIT_EXCEEDED", "MONTHLY_LIMIT_EXCEEDED"
                 -> PushCodeType.PAY_FAILED_LIMIT.getCode();

             // 카드 오류
             case "INVALID_CARD_NUMBER", "INVALID_CARD_EXPIRATION", "INVALID_CVV",
                  "CARD_EXPIRED", "CARD_RESTRICTED", "CARD_LOST_OR_STOLEN"
                 -> PushCodeType.PAY_FAILED_CARD.getCode();

             // 기타
             default -> PushCodeType.PAY_FAILED_RETRY.getCode();
         };
     }
     // ========== 푸시알림 추가 끝 ==========
}