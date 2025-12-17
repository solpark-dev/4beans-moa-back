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

    private final PushService pushService;
    private final ProductDao productDao;

    @Override
    public Deposit createDeposit(
            Integer partyId,
            Integer partyMemberId,
            String userId,
            Integer amount,
            PaymentRequest request) {

        if (partyDao.findById(partyId).isEmpty()) {
            throw new BusinessException(ErrorCode.PARTY_NOT_FOUND);
        }
        if (amount <= 0) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_AMOUNT);
        }

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

        try {
            tossPaymentService.confirmPayment(
                    request.getTossPaymentKey(),
                    request.getOrderId(),
                    amount);
        } catch (Exception e) {
            depositDao.deleteById(pendingDeposit.getDepositId());
            log.error("Toss 결제 실패, PENDING 삭제: depositId={}", pendingDeposit.getDepositId());
            throw e;
        }
        try {
            pendingDeposit.setDepositStatus(DepositStatus.PAID);
            pendingDeposit.setPaymentDate(LocalDateTime.now());
            depositDao.updateDeposit(pendingDeposit);
            log.info("PAID 상태로 업데이트 완료: depositId={}", pendingDeposit.getDepositId());
        } catch (Exception e) {
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
    	
        if (partyDao.findById(partyId).isEmpty()) {
            throw new BusinessException(ErrorCode.PARTY_NOT_FOUND);
        }

        if (amount <= 0) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_AMOUNT);
        }

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

        Deposit deposit = depositDao.findById(depositId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEPOSIT_NOT_FOUND));

        if (deposit.getDepositStatus() == DepositStatus.REFUNDED) {
            log.warn("이미 환불된 보증금: depositId={}", depositId);
            throw new BusinessException(ErrorCode.DEPOSIT_ALREADY_REFUNDED);
        }

        try {
            tossPaymentService.cancelPayment(
                    deposit.getTossPaymentKey(),
                    reason,
                    deposit.getDepositAmount());
            log.info("Toss 결제 취소 성공: paymentKey={}", deposit.getTossPaymentKey());
        } catch (com.moa.common.exception.TossPaymentException e) {
            log.error("Toss 결제 취소 실패: depositId={}, code={}, message={}",
                    depositId, e.getTossErrorCode(), e.getMessage());
            refundRetryService.recordFailure(deposit, e, reason);
            throw e;
        } catch (Exception e) {
            log.error("Toss 결제 취소 실패: depositId={}, error={}", depositId, e.getMessage());
            refundRetryService.recordFailure(deposit, e, reason);
            throw e;
        }

        deposit.setDepositStatus(DepositStatus.REFUNDED);
        deposit.setRefundDate(LocalDateTime.now());
        deposit.setRefundAmount(deposit.getDepositAmount());

        depositDao.updateDeposit(deposit);
        eventPublisher.publishEvent(new RefundCompletedEvent(
                deposit.getDepositId(),
                deposit.getRefundAmount(),
                deposit.getUserId()));
        sendDepositRefundedPush(deposit);

        log.info("보증금 환불 완료: depositId={}, amount={}", depositId, deposit.getRefundAmount());
    }

    @Override
    public void processWithdrawalRefund(Integer depositId, Party party) {
        log.info("파티 탈퇴 보증금 처리 시작: depositId={}, partyId={}", depositId, party.getPartyId());

        Deposit deposit = depositDao.findById(depositId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEPOSIT_NOT_FOUND));
        if (deposit.getDepositStatus() != DepositStatus.PAID) {
            log.info("이미 처리된 보증금: depositId={}, status={}", depositId, deposit.getDepositStatus());
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime partyStart = party.getStartDate();
        if (partyStart == null) {
            refundDeposit(depositId, "파티 탈퇴 (전액 환불)");
            return;
        }

        long daysUntilStart = java.time.temporal.ChronoUnit.DAYS.between(now.toLocalDate(), partyStart.toLocalDate());
        if (daysUntilStart >= 2) {
            refundDeposit(depositId, "파티 탈퇴 (전액 환불)");
        } else {
            forfeitDeposit(depositId, "파티 탈퇴 (전액 몰수)");
        }
    }

    @Override
    public void forfeitDeposit(Integer depositId, String reason) {
        Deposit deposit = depositDao.findById(depositId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEPOSIT_NOT_FOUND));

        if (deposit.getDepositStatus() != DepositStatus.PAID) {
            throw new BusinessException(ErrorCode.DEPOSIT_ALREADY_REFUNDED);
        }

        deposit.setDepositStatus(DepositStatus.FORFEITED);
        deposit.setRefundDate(LocalDateTime.now());
        deposit.setRefundAmount(0);

        depositDao.updateDeposit(deposit);
        sendDepositForfeitedPush(deposit);

    }

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
}