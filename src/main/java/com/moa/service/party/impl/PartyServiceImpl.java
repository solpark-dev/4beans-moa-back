package com.moa.service.party.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moa.common.exception.BusinessException;
import com.moa.common.exception.ErrorCode;
import com.moa.dao.party.PartyDao;
import com.moa.dao.partymember.PartyMemberDao;
import com.moa.dao.product.ProductDao;
import com.moa.domain.Deposit;
import com.moa.domain.Party;
import com.moa.domain.PartyMember;
import com.moa.domain.Product;
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

/**
 * 파티 서비스 구현체
 *
 * v1.0 핵심 프로세스:
 * 1. 파티 생성 → PENDING_PAYMENT
 * 2. 방장 보증금 결제 → RECRUITING
 * 3. 파티원 가입 (보증금 + 첫 달 결제) → ACTIVE
 */
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

    public PartyServiceImpl(
            PartyDao partyDao,
            PartyMemberDao partyMemberDao,
            ProductDao productDao,
            DepositService depositService,
            PaymentService paymentService,
            PushService pushService,
            com.moa.service.payment.TossPaymentService tossPaymentService,
            com.moa.service.refund.RefundRetryService refundRetryService) {
        this.partyDao = partyDao;
        this.partyMemberDao = partyMemberDao;
        this.productDao = productDao;
        this.depositService = depositService;
        this.paymentService = paymentService;
        this.pushService = pushService;
        this.tossPaymentService = tossPaymentService;
        this.refundRetryService = refundRetryService;
    }

    @Override
    public PartyDetailResponse createParty(String userId, PartyCreateRequest request) {
        // 1. 입력값 검증
        validateCreateRequest(request);

        // 2. 상품 정보 조회 (임시: 상품이 없어도 진행)
        Product product = null;
        try {
            product = productDao.getProduct(request.getProductId());
        } catch (Exception e) {
            // 무시
        }

        // 임시: 상품이 없으면 더미 데이터 사용
        if (product == null) {
            product = new Product();
            product.setProductId(request.getProductId());
            product.setProductName("Unknown Product");
            product.setPrice(10000); // 기본값
        }

        int monthlyFee = product.getPrice() / request.getMaxMembers();

        // 3. Party 엔티티 생성
        Party party = Party.builder()
                .productId(request.getProductId())
                .partyLeaderId(userId)
                .partyStatus(PartyStatus.PENDING_PAYMENT) // 초기 상태
                .maxMembers(request.getMaxMembers())
                .currentMembers(1) // 방장 포함
                .monthlyFee(monthlyFee)
                .ottId(request.getOttId()) // null 가능
                .ottPassword(request.getOttPassword()) // null 가능
                .accountId(request.getAccountId())
                .regDate(LocalDateTime.now())
                .startDate(request.getStartDate().atStartOfDay()) // LocalDate → LocalDateTime
                .endDate(request.getEndDate() != null ? request.getEndDate().atStartOfDay() : null) // LocalDate →
                                                                                                    // LocalDateTime
                                                                                                    // (null 허용)
                .build();

        // 4. PARTY 테이블 INSERT
        partyDao.insertParty(party);

        // 5. PARTY_MEMBER 테이블 INSERT (방장)
        PartyMember leaderMember = PartyMember.builder()
                .partyId(party.getPartyId())
                .userId(userId)
                .memberRole("LEADER")
                .memberStatus(MemberStatus.PENDING_PAYMENT) // 보증금 결제 대기
                .joinDate(LocalDateTime.now())
                .build();
        partyMemberDao.insertPartyMember(leaderMember);

        // 6. 생성된 파티 상세 정보 반환
        return partyDao.findDetailById(party.getPartyId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTY_NOT_FOUND));
    }

    /**
     * 방장 보증금 결제 처리
     *
     * v1.0 프로세스:
     * 1. 보증금 결제 (월구독료 전액)
     * 2. DEPOSIT 생성
     * 3. PARTY_MEMBER 상태 → ACTIVE
     * 4. PARTY 상태 → RECRUITING
     *
     * 주의사항:
     * - 방장은 월 결제를 하지 않으므로 PAYMENT 테이블에 기록하지 않음
     * - 실제 구독료(넷플릭스 등)는 방장이 별도로 결제
     * - 파티 종료 시 보증금은 결제 취소로 환불
     */
    @Override
    public PartyDetailResponse processLeaderDeposit(
            Integer partyId,
            String userId,
            PaymentRequest paymentRequest) {

        // 1. 파티 조회
        Party party = partyDao.findById(partyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTY_NOT_FOUND));

        // 2. 방장 권한 확인
        if (!party.getPartyLeaderId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_PARTY_LEADER);
        }

        // 3. 파티 상태 확인 (PENDING_PAYMENT만 가능)
        if (party.getPartyStatus() != PartyStatus.PENDING_PAYMENT) {
            throw new BusinessException(ErrorCode.INVALID_PARTY_STATUS);
        }

        // 4. 파티 멤버 조회 (방장)
        PartyMember leaderMember = partyMemberDao.findByPartyIdAndUserId(partyId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTY_MEMBER_NOT_FOUND));

        // 5. 보증금 금액 = 월구독료 전액 (상품 원가)
        // monthlyFee는 1인당 금액이므로, 전체 상품 가격 = monthlyFee * maxMembers
        int depositAmount = party.getMonthlyFee() * party.getMaxMembers();

        // 6. 보증금 생성 (결제 처리)
        depositService.createDeposit(
                partyId,
                leaderMember.getPartyMemberId(),
                userId,
                depositAmount,
                paymentRequest);

        // 6-1. PAYMENT 테이블에도 기록 (사용자 요청)
        // 방장의 보증금 결제 내역을 PAYMENT 테이블에도 저장 (PAYMENT_TYPE = 'DEPOSIT')
        String targetMonth = party.getStartDate().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        paymentService.createDepositPayment(
                partyId,
                leaderMember.getPartyMemberId(),
                userId,
                depositAmount,
                targetMonth,
                paymentRequest);

        // 7. PARTY_MEMBER 업데이트 (상태만 변경)
        // 보증금 정보는 DEPOSIT 테이블에서 userId+partyId로 조회
        leaderMember.setMemberStatus(MemberStatus.ACTIVE);
        partyMemberDao.updatePartyMember(leaderMember);

        // 8. PARTY 업데이트 (상태만 변경)
        // 방장 보증금 정보는 DEPOSIT 테이블에서 partyId+partyLeaderId로 조회
        party.setPartyStatus(PartyStatus.RECRUITING);
        partyDao.updateParty(party);

        // 9. 업데이트된 파티 정보 반환
        return partyDao.findDetailById(partyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTY_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public PartyDetailResponse getPartyDetail(Integer partyId, String userId) {
        PartyDetailResponse response = partyDao.findDetailById(partyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTY_NOT_FOUND));

        // 민감 정보 보호: 파티원이나 방장이 아니면 OTT 정보 숨김
        boolean isMember = false;
        if (userId != null) {
            // 방장인지 확인
            if (response.getPartyLeaderId().equals(userId)) {
                isMember = true;
            } else {
                // 멤버인지 확인 및 상태 저장
                partyMemberDao.findByPartyIdAndUserId(partyId, userId)
                        .ifPresent(member -> {
                            response.setMemberStatus(member.getMemberStatus());
                        });

                // 활성 멤버인 경우에만 멤버 권한 부여
                if (response.getMemberStatus() == MemberStatus.ACTIVE) {
                    isMember = true;
                }
            }
        }

        if (!isMember) {
            response.setOttId(null);
            response.setOttPassword(null);
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PartyListResponse> getPartyList(
            Integer productId,
            String partyStatus,
            String keyword,
            int page,
            int size) {

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

        return partyDao.findPartyList(productId, status, keyword, offset, size);
    }

    @Override
    public PartyDetailResponse updateOttAccount(
            Integer partyId,
            String userId,
            UpdateOttAccountRequest request) {

        // 1. 파티 조회
        Party party = partyDao.findById(partyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTY_NOT_FOUND));

        // 2. 방장 권한 확인
        if (!party.getPartyLeaderId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_PARTY_LEADER);
        }

        // 3. OTT 계정 정보 업데이트
        partyDao.updateOttAccount(partyId, request.getOttId(), request.getOttPassword());

        // 4. 수정된 파티 정보 반환
        return partyDao.findDetailById(partyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTY_NOT_FOUND));
    }

    /**
     * 파티원 가입 처리 (동시성 제어 개선)
     *
     * v1.1 프로세스 (정원 증가 우선):
     * 1. 정원 증가 (동시성 제어) - Toss 호출 전에 먼저 실행
     * 2. Toss 결제 승인
     * 3. DEPOSIT, PAYMENT 생성
     * 4. PARTY_MEMBER 상태 → ACTIVE
     * 5. 최대 인원 도달 시 PARTY 상태 → ACTIVE
     * 
     * 실패 시 복구:
     * - Toss 실패: 정원 복구 (decrementCurrentMembers)
     * - DB 실패: 정원 복구 + 보상 트랜잭션 등록
     */
    @Override
    public PartyMemberResponse joinParty(
            Integer partyId,
            String userId,
            PaymentRequest paymentRequest) {

        // 1. 파티 조회
        Party party = partyDao.findById(partyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTY_NOT_FOUND));

        // 2. 상태 확인 (RECRUITING만 가능)
        if (party.getPartyStatus() != PartyStatus.RECRUITING) {
            throw new BusinessException(ErrorCode.PARTY_NOT_RECRUITING);
        }

        // 3. 방장 본인 참여 방지
        if (party.getPartyLeaderId().equals(userId)) {
            throw new BusinessException(ErrorCode.LEADER_CANNOT_JOIN);
        }

        // 4. 중복 가입 확인
        partyMemberDao.findByPartyIdAndUserId(partyId, userId)
                .ifPresent(member -> {
                    throw new BusinessException(ErrorCode.ALREADY_JOINED);
                });

        // 5. [동시성 제어] 정원 증가 먼저 실행 (Toss 호출 전)
        int updatedRows = partyDao.incrementCurrentMembers(partyId);
        if (updatedRows == 0) {
            // 이미 만석 - Toss 호출하지 않음
            throw new BusinessException(ErrorCode.PARTY_FULL);
        }

        // 6. 인당 요금 (monthlyFee는 이미 1인당 금액으로 저장되어 있음)
        int fee = party.getMonthlyFee();

        // 7. PARTY_MEMBER 생성 (PENDING_PAYMENT)
        PartyMember partyMember = PartyMember.builder()
                .partyId(partyId)
                .userId(userId)
                .memberRole("MEMBER")
                .memberStatus(MemberStatus.PENDING_PAYMENT)
                .joinDate(LocalDateTime.now())
                .build();
        partyMemberDao.insertPartyMember(partyMember);

        // 8. Toss Payments 결제 승인 (보증금 + 첫 달 구독료)
        int totalAmount = fee * 2;
        try {
            tossPaymentService.confirmPayment(
                    paymentRequest.getTossPaymentKey(),
                    paymentRequest.getOrderId(),
                    totalAmount);
        } catch (Exception e) {
            // Toss 실패 시 정원 복구
            partyDao.decrementCurrentMembers(partyId);
            partyMemberDao.deletePartyMember(partyMember.getPartyMemberId());
            log.error("Toss 결제 실패, 정원 복구: partyId={}, error={}", partyId, e.getMessage());
            throw e;
        }

        // 9. 보증금 기록 생성
        try {
            depositService.createDepositWithoutConfirm(
                    partyId,
                    partyMember.getPartyMemberId(),
                    userId,
                    fee,
                    paymentRequest);
        } catch (Exception e) {
            // DB 실패 시 정원 복구 + 보상 트랜잭션 등록
            partyDao.decrementCurrentMembers(partyId);
            Deposit pendingDeposit = Deposit.builder()
                    .depositId(null)
                    .partyId(partyId)
                    .userId(userId)
                    .depositAmount(fee)
                    .tossPaymentKey(paymentRequest.getTossPaymentKey())
                    .build();
            refundRetryService.recordCompensation(pendingDeposit, "Toss 성공 후 Deposit 생성 실패");
            log.error("Deposit 생성 실패, 보상 트랜잭션 등록: partyId={}", partyId);
            throw e;
        }

        // 10. 첫 달 결제 생성
        String targetMonth = party.getStartDate().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        paymentService.createInitialPaymentWithoutConfirm(
                partyId,
                partyMember.getPartyMemberId(),
                userId,
                fee,
                targetMonth,
                paymentRequest);

        // 11. PARTY_MEMBER 업데이트 (상태만 변경)
        // 보증금/결제 정보는 DEPOSIT, PAYMENT 테이블에서 조회
        partyMember.setMemberStatus(MemberStatus.ACTIVE);
        partyMemberDao.updatePartyMember(partyMember);

        // 12. 최대 인원 도달 시 ACTIVE로 전환
        Party updatedParty = partyDao.findById(partyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTY_NOT_FOUND));

        if (updatedParty.getCurrentMembers() >= updatedParty.getMaxMembers()) {
            partyDao.updatePartyStatus(partyId, PartyStatus.ACTIVE);
            safeSendPush(() -> sendPartyStartPushToAllMembers(partyId, updatedParty));
        }

        // 13. 파티 가입 완료 알림 발송
        safeSendPush(() -> sendPartyJoinPush(userId, userId, party));

        return partyMemberDao.findByPartyMemberId(partyMember.getPartyMemberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTY_MEMBER_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PartyMemberResponse> getPartyMembers(Integer partyId) {
        // 파티 존재 확인
        partyDao.findById(partyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTY_NOT_FOUND));

        return partyMemberDao.findMembersByPartyId(partyId);
    }

    @Override
    public void leaveParty(Integer partyId, String userId) {
        // 1. 파티 조회
        Party party = partyDao.findById(partyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTY_NOT_FOUND));

        // 2. 파티장은 탈퇴 불가
        if (party.getPartyLeaderId().equals(userId)) {
            throw new BusinessException(ErrorCode.LEADER_CANNOT_LEAVE);
        }

        // 3. 파티 멤버 조회
        PartyMember member = partyMemberDao.findByPartyIdAndUserId(partyId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_PARTY_MEMBER));

        // 4. 이미 탈퇴한 멤버인지 확인
        if (member.getMemberStatus() != MemberStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.NOT_PARTY_MEMBER);
        }

        // 5. 파티 멤버 상태를 INACTIVE로 변경
        member.setMemberStatus(MemberStatus.INACTIVE);
        member.setWithdrawDate(LocalDateTime.now());
        partyMemberDao.updatePartyMember(member);

        // 6. 파티 현재 인원 감소
        int updatedRows = partyDao.decrementCurrentMembers(partyId);
        if (updatedRows == 0) {
            throw new BusinessException(ErrorCode.PARTY_NOT_FOUND);
        }

        // 7. 보증금 처리 (정책에 따라 환불/부분환불/몰수)
        // PARTY_MEMBER 테이블에 DEPOSIT_ID가 없으므로 DEPOSIT 테이블에서 조회
        Deposit memberDeposit = depositService.findByPartyIdAndUserId(partyId, userId);
        if (memberDeposit != null) {
            try {
                depositService.processWithdrawalRefund(memberDeposit.getDepositId(), party);
            } catch (Exception e) {
                // 환불 실패 시 로그만 남기고 계속 진행 (추후 수동 처리)
                System.err.println("보증금 처리 실패: " + e.getMessage());
            }
        }

        // 8. 파티 시작 전이면 구독료(월 분담금) 환불
        if (party.getStartDate().isAfter(LocalDateTime.now())) {
            try {
                paymentService.refundPayment(partyId, member.getPartyMemberId(), "파티 시작 전 탈퇴 (구독료 환불)");
            } catch (Exception e) {
                System.err.println("구독료 환불 실패: " + e.getMessage());
            }
        }

        // 9. 파티 상태 업데이트 (ACTIVE → RECRUITING으로 변경 가능)
        Party updatedParty = partyDao.findById(partyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTY_NOT_FOUND));

        if (updatedParty.getPartyStatus() == PartyStatus.ACTIVE
                && updatedParty.getCurrentMembers() < updatedParty.getMaxMembers()) {
            partyDao.updatePartyStatus(partyId, PartyStatus.RECRUITING);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PartyListResponse> getMyParties(String userId) {
        return partyDao.findMyParties(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PartyListResponse> getMyLeadingParties(String userId) {
        return partyDao.findMyLeadingParties(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PartyListResponse> getMyParticipatingParties(String userId) {
        return partyDao.findMyParticipatingParties(userId);
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
        // OTT ID/PW 검증 제거 (생성 시점에는 선택 사항)
    }

    private int calculateLastMemberFee(int monthlyFee, int maxMembers) {
        // 마지막 파티원: 나머지 포함
        int perPersonFee = monthlyFee / maxMembers;
        return monthlyFee - (perPersonFee * (maxMembers - 1));
    }

    // ========== ⭐ Private Push 메서드 (PARTY_JOIN, PARTY_START만) ==========

    private void safeSendPush(Runnable pushAction) {
        try {
            pushAction.run();
        } catch (Exception e) {
            System.err.println("Push 발송 실패 (무시): " + e.getMessage());
        }
    }

    private void sendPartyJoinPush(String userId, String nickname, Party party) {
        TemplatePushRequest pushRequest = TemplatePushRequest.builder()
                .receiverId(userId)
                .pushCode(PushCodeType.PARTY_JOIN.getCode())
                .params(Map.of(
                        "nickname", nickname,
                        "product_name", getProductName(party.getProductId())))
                .moduleId(String.valueOf(party.getPartyId()))
                .moduleType(PushCodeType.PARTY_JOIN.getModuleType())
                .build();

        pushService.addTemplatePush(pushRequest);
    }
    
    private void sendPartyStartPushToAllMembers(Integer partyId, Party party) {
        List<PartyMemberResponse> members = partyMemberDao.findMembersByPartyId(partyId);
        String productName = getProductName(party.getProductId());

        for (PartyMemberResponse member : members) {
            TemplatePushRequest pushRequest = TemplatePushRequest.builder()
                    .receiverId(member.getUserId())
                    .pushCode(PushCodeType.PARTY_START.getCode())
                    .params(Map.of("product_name", productName))
                    .moduleId(String.valueOf(partyId))
                    .moduleType(PushCodeType.PARTY_START.getModuleType())
                    .build();

            pushService.addTemplatePush(pushRequest);
        }
    }

    private String getProductName(Integer productId) {
        try {
            Product product = productDao.getProduct(productId);
            return product.getProductName();
        } catch (Exception e) {
            return "Unknown Product";
        }
    }
}