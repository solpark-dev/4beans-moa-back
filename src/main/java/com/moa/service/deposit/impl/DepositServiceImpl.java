package com.moa.service.deposit.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moa.common.event.RefundCompletedEvent;
import com.moa.common.exception.BusinessException;
import com.moa.common.exception.ErrorCode;
import com.moa.dao.deposit.DepositDao;
import com.moa.dao.party.PartyDao;
import com.moa.dao.product.ProductDao;
import com.moa.domain.Deposit;
import com.moa.domain.Party;
import com.moa.domain.Product;
import com.moa.domain.enums.DepositStatus;
import com.moa.domain.enums.PushCodeType;
import com.moa.dto.deposit.response.DepositResponse;
import com.moa.dto.payment.request.PaymentRequest;
import com.moa.dto.push.request.TemplatePushRequest;
import com.moa.service.deposit.DepositService;
import com.moa.service.payment.TossPaymentService;
import com.moa.service.push.PushService;
import com.moa.service.refund.RefundRetryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 보증금 서비스 구현체
 *
 * 보증금 규칙:
 * - 방장: 월구독료 전액
 * - 파티원: 인당 요금
 * 
 * 탈퇴 시 환불 정책:
 * - 파티 시작 2일 전까지: 전액 환불
 * - 파티 시작 1일 전부터: 전액 몰수 (다음 정산에 포함)
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class DepositServiceImpl implements DepositService {

    private final DepositDao depositDao;
    private final PartyDao partyDao;
    private final TossPaymentService tossPaymentService;
    private final ApplicationEventPublisher eventPublisher;
    private final com.moa.dao.refund.RefundRetryHistoryDao refundRetryHistoryDao;
    private final RefundRetryService refundRetryService;
    
    // ========== 푸시알림 추가 ==========
    private final PushService pushService;
    private final ProductDao productDao;
    // ========== 푸시알림 추가 끝 ==========

    @Override
    public Deposit createDeposit(
            Integer partyId,
            Integer partyMemberId,
            String userId,
            Integer amount,
            PaymentRequest request) {

        // 1. 파티 존재 확인
        if (partyDao.findById(partyId).isEmpty()) {
            throw new BusinessException(ErrorCode.PARTY_NOT_FOUND);
        }

        // 2. 금액 검증
        if (amount <= 0) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_AMOUNT);
        }

        // 3. [2단계 저장 패턴] PENDING 상태로 먼저 저장
        Deposit pendingDeposit = Deposit.builder()
                .partyId(partyId)
                .partyMemberId(partyMemberId)
                .userId(userId)
                .depositType("SECURITY")
                .depositAmount(amount)
                .depositStatus(DepositStatus.PENDING)
                .transactionDate(LocalDateTime.now())
                .paymentDate(LocalDateTime.now())
                .tossPaymentKey(request.getTossPaymentKey())
                .orderId(request.getOrderId())
                .build();
        depositDao.insertDeposit(pendingDeposit);
        log.info("PENDING 상태 Deposit 생성: depositId={}", pendingDeposit.getDepositId());

        // 4. Toss Payments 결제 승인
        try {
            tossPaymentService.confirmPayment(
                    request.getTossPaymentKey(),
                    request.getOrderId(),
                    amount);
        } catch (Exception e) {
            // Toss 실패 시 PENDING 레코드 삭제
            depositDao.deleteById(pendingDeposit.getDepositId());
            log.error("Toss 결제 실패, PENDING 삭제: depositId={}", pendingDeposit.getDepositId());
            throw e;
        }

        // 5. [2단계 저장 패턴] PAID 상태로 업데이트
        try {
            pendingDeposit.setDepositStatus(DepositStatus.PAID);
            pendingDeposit.setPaymentDate(LocalDateTime.now());
            depositDao.updateDeposit(pendingDeposit);
            log.info("PAID 상태로 업데이트 완료: depositId={}", pendingDeposit.getDepositId());
        } catch (Exception e) {
            // DB 업데이트 실패 시 보상 트랜잭션 등록
            refundRetryService.recordCompensation(pendingDeposit, "Toss 성공 후 DB 업데이트 실패");
            log.error("DB 업데이트 실패, 보상 트랜잭션 등록: depositId={}", pendingDeposit.getDepositId());
            throw e;
        }

        return pendingDeposit;
    }

    @Override
    public Deposit createDepositWithoutConfirm(
            Integer partyId,
            Integer partyMemberId,
            String userId,
            Integer amount,
            PaymentRequest request) {

        // 1. 파티 존재 확인
        if (partyDao.findById(partyId).isEmpty()) {
            throw new BusinessException(ErrorCode.PARTY_NOT_FOUND);
        }

        // 2. 금액 검증
        if (amount <= 0) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_AMOUNT);
        }

        // 3. Toss 승인은 이미 joinParty에서 완료됨 - 생략

        // 4. Deposit 엔티티 생성
        Deposit deposit = Deposit.builder()
                .partyId(partyId)
                .partyMemberId(partyMemberId)
                .userId(userId)
                .depositType("SECURITY")
                .depositAmount(amount)
                .depositStatus(DepositStatus.PAID)
                .paymentDate(LocalDateTime.now())
                .transactionDate(LocalDateTime.now())
                .tossPaymentKey(request.getTossPaymentKey())
                .orderId(request.getOrderId())
                .build();

        // 5. DB 저장
        depositDao.insertDeposit(deposit);

        return deposit;
    }

    @Override
    @Transactional(readOnly = true)
    public DepositResponse getDepositDetail(Integer depositId) {
        return depositDao.findDetailById(depositId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEPOSIT_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepositResponse> getMyDeposits(String userId) {
        return depositDao.findByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepositResponse> getPartyDeposits(Integer partyId) {
        return depositDao.findByPartyId(partyId);
    }

    @Override
    @Transactional(readOnly = true)
    public Deposit findByPartyIdAndUserId(Integer partyId, String userId) {
        return depositDao.findByPartyIdAndUserId(partyId, userId).orElse(null);
    }

    @Override
    public void refundDeposit(Integer depositId, String reason) {
        log.info("보증금 환불 시작: depositId={}, reason={}", depositId, reason);

        // 1. 보증금 조회
        Deposit deposit = depositDao.findById(depositId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEPOSIT_NOT_FOUND));

        // 2. 이미 환불되었는지 확인
        if (deposit.getDepositStatus() == DepositStatus.REFUNDED) {
            log.warn("이미 환불된 보증금: depositId={}", depositId);
            throw new BusinessException(ErrorCode.DEPOSIT_ALREADY_REFUNDED);
        }

        // 3. Toss Payments 결제 취소 API 호출
        // 3. Toss Payments 결제 취소 API 호출
        try {
            tossPaymentService.cancelPayment(
                    deposit.getTossPaymentKey(),
                    reason,
                    deposit.getDepositAmount());
            log.info("Toss 결제 취소 성공: paymentKey={}", deposit.getTossPaymentKey());
        } catch (com.moa.common.exception.TossPaymentException e) {
            log.error("Toss 결제 취소 실패: depositId={}, code={}, message={}",
                    depositId, e.getTossErrorCode(), e.getMessage());
            // REQUIRES_NEW 트랜잭션으로 실패 이력 기록 (에러 코드 포함)
            // RefundRetryService.recordFailure가 Exception을 받으므로 그대로 전달
            refundRetryService.recordFailure(deposit, e, reason);
            throw e;
        } catch (Exception e) {
            log.error("Toss 결제 취소 실패: depositId={}, error={}", depositId, e.getMessage());
            // REQUIRES_NEW 트랜잭션으로 실패 이력 기록
            refundRetryService.recordFailure(deposit, e, reason);
            throw e;
        }

        // 4. 상태 업데이트
        deposit.setDepositStatus(DepositStatus.REFUNDED);
        deposit.setRefundDate(LocalDateTime.now());
        deposit.setRefundAmount(deposit.getDepositAmount());

        depositDao.updateDeposit(deposit);

        // 5. 이벤트 발행
        eventPublisher.publishEvent(new RefundCompletedEvent(
                deposit.getDepositId(),
                deposit.getRefundAmount(),
                deposit.getUserId()));

        // ========== 푸시알림 추가: 보증금 환불 완료 ==========
        sendDepositRefundedPush(deposit);
        // ========== 푸시알림 추가 끝 ==========

        log.info("보증금 환불 완료: depositId={}, amount={}", depositId, deposit.getRefundAmount());
    }

    @Override
    public void processWithdrawalRefund(Integer depositId, Party party) {
        log.info("파티 탈퇴 보증금 처리 시작: depositId={}, partyId={}", depositId, party.getPartyId());

        Deposit deposit = depositDao.findById(depositId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEPOSIT_NOT_FOUND));

        // 이미 처리된 경우 무시
        if (deposit.getDepositStatus() != DepositStatus.PAID) {
            log.info("이미 처리된 보증금: depositId={}, status={}", depositId, deposit.getDepositStatus());
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime partyStart = party.getStartDate();

        // 파티 시작일이 null인 경우 전액 환불
        if (partyStart == null) {
            refundDeposit(depositId, "파티 탈퇴 (전액 환불)");
            return;
        }

        long daysUntilStart = java.time.temporal.ChronoUnit.DAYS.between(now.toLocalDate(), partyStart.toLocalDate());

        // 간소화된 환불 정책: 2일 전까지 전액 환불, 1일 전부터 전액 몰수
        if (daysUntilStart >= 2) {
            // 파티 시작 2일 전까지: 전액 환불
            refundDeposit(depositId, "파티 탈퇴 (전액 환불)");
        } else {
            // 파티 시작 1일 전부터: 전액 몰수
            forfeitDeposit(depositId, "파티 탈퇴 (전액 몰수)");
        }
    }

    @Override
    public void forfeitDeposit(Integer depositId, String reason) {
        Deposit deposit = depositDao.findById(depositId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEPOSIT_NOT_FOUND));

        // 이미 처리된 경우 예외
        if (deposit.getDepositStatus() != DepositStatus.PAID) {
            throw new BusinessException(ErrorCode.DEPOSIT_ALREADY_REFUNDED);
        }

        // 몰수 - Toss 취소 없음 (돈은 이미 MOA에 있음)
        deposit.setDepositStatus(DepositStatus.FORFEITED);
        deposit.setRefundDate(LocalDateTime.now());
        deposit.setRefundAmount(0);

        depositDao.updateDeposit(deposit);

        // ========== 푸시알림 추가: 보증금 몰수 ==========
        sendDepositForfeitedPush(deposit);
        // ========== 푸시알림 추가 끝 ==========
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
     * 푸시알림 추가: 보증금 환불 완료 알림
     */
    private void sendDepositRefundedPush(Deposit deposit) {
        try {
            Party party = partyDao.findById(deposit.getPartyId()).orElse(null);
            if (party == null) return;

            String productName = getProductName(party.getProductId());

            Map<String, String> params = Map.of(
                "productName", productName,
                "amount", String.valueOf(deposit.getRefundAmount())
            );

            TemplatePushRequest pushRequest = TemplatePushRequest.builder()
                .receiverId(deposit.getUserId())
                .pushCode(PushCodeType.DEPOSIT_REFUNDED.getCode())
                .params(params)
                .moduleId(String.valueOf(deposit.getDepositId()))
                .moduleType(PushCodeType.DEPOSIT_REFUNDED.getModuleType())
                .build();

            pushService.addTemplatePush(pushRequest);
            log.info("푸시알림 발송 완료: DEPOSIT_REFUNDED -> userId={}", deposit.getUserId());

        } catch (Exception e) {
            log.error("푸시알림 발송 실패: depositId={}, error={}", deposit.getDepositId(), e.getMessage());
        }
    }

    /**
     * 푸시알림 추가: 보증금 몰수 알림
     */
    private void sendDepositForfeitedPush(Deposit deposit) {
        try {
            Party party = partyDao.findById(deposit.getPartyId()).orElse(null);
            if (party == null) return;

            String productName = getProductName(party.getProductId());

            Map<String, String> params = Map.of(
                "productName", productName,
                "amount", String.valueOf(deposit.getDepositAmount())
            );

            TemplatePushRequest pushRequest = TemplatePushRequest.builder()
                .receiverId(deposit.getUserId())
                .pushCode(PushCodeType.DEPOSIT_FORFEITED.getCode())
                .params(params)
                .moduleId(String.valueOf(deposit.getDepositId()))
                .moduleType(PushCodeType.DEPOSIT_FORFEITED.getModuleType())
                .build();

            pushService.addTemplatePush(pushRequest);
            log.info("푸시알림 발송 완료: DEPOSIT_FORFEITED -> userId={}", deposit.getUserId());

        } catch (Exception e) {
            log.error("푸시알림 발송 실패: depositId={}, error={}", deposit.getDepositId(), e.getMessage());
        }
    }
    // ========== 푸시알림 추가 끝 ==========
}