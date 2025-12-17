package com.moa.event.listener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.moa.common.event.UserDeletedEvent;
import com.moa.dao.deposit.DepositDao;
import com.moa.dao.party.PartyDao;
import com.moa.dao.partymember.PartyMemberDao;
import com.moa.dao.product.ProductDao;
import com.moa.domain.Deposit;
import com.moa.domain.Party;
import com.moa.domain.PartyMember;
import com.moa.domain.Product;
import com.moa.domain.enums.DepositStatus;
import com.moa.domain.enums.MemberStatus;
import com.moa.domain.enums.PartyStatus;
import com.moa.domain.enums.PushCodeType;
import com.moa.dto.partymember.response.PartyMemberResponse;
import com.moa.dto.push.request.TemplatePushRequest;
import com.moa.service.deposit.DepositService;
import com.moa.service.push.PushService;
import com.moa.service.refund.RefundRetryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserDeletionEventListener {

	private final PartyDao partyDao;
	private final PartyMemberDao partyMemberDao;
	private final DepositDao depositDao;
	private final DepositService depositService;
	private final RefundRetryService refundRetryService;
	private final PushService pushService;
	private final ProductDao productDao;

	@EventListener
	@Async
	@Transactional
	public void handleUserDeleted(UserDeletedEvent event) {
		log.info("===== 사용자 삭제 이벤트 처리 시작 =====");
		log.info("Event: {}", event);

		String userId = event.getUserId();
		String deleteReason = event.getDeleteReason() != null ? event.getDeleteReason() : "사용자 탈퇴";

		try {
			handleLeaderParties(userId, deleteReason);
			handleMemberParties(userId, deleteReason);

			log.info("===== 사용자 삭제 이벤트 처리 완료 =====");

		} catch (Exception e) {
			log.error("사용자 삭제 이벤트 처리 실패: userId={}, error={}", userId, e.getMessage(), e);

		}
	}

	private void handleLeaderParties(String userId, String deleteReason) {
		log.info("파티장 파티 처리 시작: userId={}", userId);

		List<Party> leaderParties = partyDao.findActivePartiesByLeaderId(userId);

		if (leaderParties.isEmpty()) {
			log.info("파티장으로 있는 활성 파티 없음");
			return;
		}

		log.info("처리할 파티장 파티 수: {}", leaderParties.size());

		for (Party party : leaderParties) {
			try {
				processLeaderPartyDisbandment(party, deleteReason);
			} catch (Exception e) {
				log.error("파티장 파티 해산 처리 실패: partyId={}, error={}", party.getPartyId(), e.getMessage(), e);
			}
		}
	}

	private void processLeaderPartyDisbandment(Party party, String deleteReason) {
		Integer partyId = party.getPartyId();
		PartyStatus status = party.getPartyStatus();

		log.info("파티장 파티 해산 처리: partyId={}, status={}", partyId, status);

		switch (status) {
		case PENDING_PAYMENT:
			log.info("PENDING_PAYMENT 파티 삭제: partyId={}", partyId);
			partyDao.updatePartyStatus(partyId, PartyStatus.DISBANDED);
			break;

		case RECRUITING:
			log.info("RECRUITING 파티 해산: partyId={}", partyId);
			disbandRecruitingParty(party, deleteReason);
			break;

		case ACTIVE:
			log.info("ACTIVE 파티 해산: partyId={}", partyId);
			disbandActiveParty(party, deleteReason);
			break;

		case SUSPENDED:
			log.info("SUSPENDED 파티 해산: partyId={}", partyId);
			disbandActiveParty(party, deleteReason);
			break;

		default:
			log.warn("처리 불필요한 파티 상태: partyId={}, status={}", partyId, status);
		}
	}

	private void disbandRecruitingParty(Party party, String deleteReason) {
		Integer partyId = party.getPartyId();

		List<PartyMemberResponse> members = partyMemberDao.findMembersByPartyId(partyId);

		for (PartyMemberResponse member : members) {
			if ("ACTIVE".equals(member.getMemberStatus())) {
				try {
					refundMemberDeposit(partyId, member.getUserId(), "파티장 탈퇴로 인한 파티 해산 (전액 환불)");

					updateMemberStatus(member.getPartyMemberId(), MemberStatus.INACTIVE);
					sendPartyDisbandedPush(member.getUserId(), party, deleteReason);

				} catch (Exception e) {
					log.error("파티원 환불 실패: partyMemberId={}, error={}", member.getPartyMemberId(), e.getMessage());
				}
			}
		}
		try {
			refundMemberDeposit(partyId, party.getPartyLeaderId(), "파티장 탈퇴로 인한 보증금 환불");
		} catch (Exception e) {
			log.error("방장 보증금 환불 실패: partyId={}, error={}", partyId, e.getMessage());
		}

		partyDao.updatePartyStatus(partyId, PartyStatus.DISBANDED);
	}

	private void disbandActiveParty(Party party, String deleteReason) {
		Integer partyId = party.getPartyId();
		List<PartyMemberResponse> members = partyMemberDao.findMembersByPartyId(partyId);

		for (PartyMemberResponse member : members) {
			if ("ACTIVE".equals(member.getMemberStatus())) {
				try {
					refundMemberDeposit(partyId, member.getUserId(), "파티장 탈퇴로 인한 강제 해산 (전액 환불)");

					updateMemberStatus(member.getPartyMemberId(), MemberStatus.INACTIVE);
					sendPartyDisbandedPush(member.getUserId(), party, deleteReason);

				} catch (Exception e) {
					log.error("파티원 환불 실패: partyMemberId={}, error={}", member.getPartyMemberId(), e.getMessage());
				}
			}
		}
		try {
			refundMemberDeposit(partyId, party.getPartyLeaderId(), "파티장 탈퇴로 인한 보증금 환불");
		} catch (Exception e) {
			log.error("방장 보증금 환불 실패: partyId={}, error={}", partyId, e.getMessage());
		}

		partyDao.updatePartyStatus(partyId, PartyStatus.DISBANDED);
	}

	private void handleMemberParties(String userId, String deleteReason) {
		log.info("파티원 파티 처리 시작: userId={}", userId);

		List<PartyMember> memberRecords = partyMemberDao.findActiveMembershipsByUserId(userId);

		if (memberRecords.isEmpty()) {
			log.info("파티원으로 있는 활성 멤버십 없음");
			return;
		}

		log.info("처리할 파티원 멤버십 수: {}", memberRecords.size());

		for (PartyMember member : memberRecords) {
			try {
				processMemberWithdrawal(member, deleteReason);
			} catch (Exception e) {
				log.error("파티원 탈퇴 처리 실패: partyMemberId={}, error={}", member.getPartyMemberId(), e.getMessage(), e);
			}
		}
	}

	private void processMemberWithdrawal(PartyMember member, String deleteReason) {
		Integer partyId = member.getPartyId();
		Integer partyMemberId = member.getPartyMemberId();
		String userId = member.getUserId();

		log.info("파티원 강제 탈퇴 처리: partyId={}, partyMemberId={}, userId={}", partyId, partyMemberId, userId);

		Party party = partyDao.findById(partyId).orElse(null);
		if (party == null) {
			log.warn("파티를 찾을 수 없음: partyId={}", partyId);
			return;
		}

		if (party.getPartyLeaderId().equals(userId)) {
			log.info("파티장이므로 파티원 처리 스킵: partyId={}", partyId);
			return;
		}
		try {
			Deposit deposit = depositDao.findByPartyIdAndUserId(partyId, userId).orElse(null);
			if (deposit != null && deposit.getDepositStatus() == DepositStatus.PAID) {
				depositService.processWithdrawalRefund(deposit.getDepositId(), party);
			}
		} catch (Exception e) {
			log.error("보증금 처리 실패: partyMemberId={}, error={}", partyMemberId, e.getMessage());
		}

		member.setMemberStatus(MemberStatus.INACTIVE);
		member.setWithdrawDate(LocalDateTime.now());
		partyMemberDao.updatePartyMember(member);
		partyDao.decrementCurrentMembers(partyId);

		Party updatedParty = partyDao.findById(partyId).orElse(null);
		if (updatedParty != null && updatedParty.getPartyStatus() == PartyStatus.ACTIVE
				&& updatedParty.getCurrentMembers() < updatedParty.getMaxMembers()) {
			partyDao.updatePartyStatus(partyId, PartyStatus.RECRUITING);
		}
		try {
			sendMemberWithdrawnPush(party.getPartyLeaderId(), userId, party, deleteReason);
		} catch (Exception e) {
			log.error("알림 발송 실패: error={}", e.getMessage());
		}
	}

	private void refundMemberDeposit(Integer partyId, String userId, String reason) {
		Deposit deposit = depositDao.findByPartyIdAndUserId(partyId, userId).orElse(null);

		if (deposit == null) {
			log.warn("보증금을 찾을 수 없음: partyId={}, userId={}", partyId, userId);
			return;
		}

		if (deposit.getDepositStatus() != DepositStatus.PAID) {
			log.warn("환불 불가능한 보증금 상태: depositId={}, status={}", deposit.getDepositId(), deposit.getDepositStatus());
			return;
		}

		try {
			depositService.refundDeposit(deposit.getDepositId(), reason);
		} catch (Exception e) {
			log.error("보증금 환불 실패, 재시도 등록: depositId={}, error={}", deposit.getDepositId(), e.getMessage());
			refundRetryService.recordFailure(deposit, e, reason);
		}
	}

	private void updateMemberStatus(Integer partyMemberId, MemberStatus status) {
		partyMemberDao.leaveParty(partyMemberId);
	}

	private String getProductName(Integer productId) {
		if (productId == null)
			return "OTT 서비스";
		try {
			Product product = productDao.getProduct(productId);
			return (product != null && product.getProductName() != null) ? product.getProductName() : "OTT 서비스";
		} catch (Exception e) {
			return "OTT 서비스";
		}
	}

	private void sendPartyDisbandedPush(String receiverId, Party party, String reason) {
		try {
			String productName = getProductName(party.getProductId());

			Map<String, String> params = Map.of("productName", productName, "reason",
					reason != null ? reason : "파티장 탈퇴");

			TemplatePushRequest pushRequest = TemplatePushRequest.builder().receiverId(receiverId)
					.pushCode(PushCodeType.PARTY_DISBANDED.getCode()).params(params)
					.moduleId(String.valueOf(party.getPartyId())).moduleType("PARTY").build();

			pushService.addTemplatePush(pushRequest);
			log.info("파티 해산 알림 발송: receiverId={}", receiverId);
		} catch (Exception e) {
			log.error("푸시 발송 실패: {}", e.getMessage());
		}
	}

	private void sendMemberWithdrawnPush(String leaderId, String withdrawnUserId, Party party, String reason) {
		try {
			String productName = getProductName(party.getProductId());

			Map<String, String> params = Map.of("productName", productName, "memberNickname", withdrawnUserId, "reason",
					reason != null ? reason : "회원 탈퇴");

			TemplatePushRequest pushRequest = TemplatePushRequest.builder().receiverId(leaderId)
					.pushCode(PushCodeType.MEMBER_WITHDRAWN.getCode()).params(params)
					.moduleId(String.valueOf(party.getPartyId())).moduleType("PARTY").build();

			pushService.addTemplatePush(pushRequest);
			log.info("파티원 탈퇴 알림 발송: leaderId={}", leaderId);
		} catch (Exception e) {
			log.error("푸시 발송 실패: {}", e.getMessage());
		}
	}
}
