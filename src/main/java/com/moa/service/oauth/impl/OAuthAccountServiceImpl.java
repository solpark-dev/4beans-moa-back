package com.moa.service.oauth.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.moa.common.exception.BusinessException;
import com.moa.common.exception.ErrorCode;
import com.moa.dao.oauth.OAuthAccountDao;
import com.moa.domain.OAuthAccount;
import com.moa.service.oauth.OAuthAccountService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OAuthAccountServiceImpl implements OAuthAccountService {

	private final OAuthAccountDao dao;

	@Override
	public OAuthAccount getOAuthAccount(String oauthId) {
		return dao.getOAuthAccount(oauthId);
	}

	@Override
	public List<OAuthAccount> getOAuthAccountList(String userId) {
		return dao.getOAuthAccountList(userId);
	}

	@Override
	public OAuthAccount getOAuthByProvider(String provider, String providerUserId) {
		return dao.getOAuthByProvider(provider, providerUserId);
	}

	@Override
	public void addOAuthAccount(OAuthAccount account) {
		OAuthAccount existing = dao.getOAuthByProvider(account.getProvider(), account.getProviderUserId());

		if (existing != null) {
			dao.reconnectOAuthAccount(existing.getOauthId());
			return;
		}

		dao.addOAuthAccount(account);
	}

	@Override
	public void releaseOAuth(String oauthId) {
		dao.updateOAuthRelease(oauthId);
	}

	@Override
	public void connectOAuthAccount(String userId, String provider, String providerUserId) {

		OAuthAccount existing = dao.getOAuthByProvider(provider, providerUserId);

		if (existing != null) {
			dao.reconnectOAuthAccount(existing.getOauthId());
			return;
		}

		OAuthAccount account = OAuthAccount.builder().oauthId(UUID.randomUUID().toString()).userId(userId)
				.provider(provider).providerUserId(providerUserId).connectedDate(LocalDateTime.now()).build();

		dao.addOAuthAccount(account);
	}

	@Override
	public void transferOAuthAccount(String provider, String providerUserId, String fromUserId, String toUserId) {

		OAuthAccount existing = dao.getOAuthByProvider(provider, providerUserId);
		if (existing == null) {
			throw new BusinessException(ErrorCode.NOT_FOUND, "연결된 소셜 계정을 찾을 수 없습니다.");
		}

		if (!fromUserId.equals(existing.getUserId())) {
			throw new BusinessException(ErrorCode.CONFLICT, "이전 대상 계정 정보가 일치하지 않습니다.");
		}

		OAuthAccount targetExisting = dao.getOAuthByUserAndProvider(toUserId, provider);
		if (targetExisting != null && !targetExisting.getOauthId().equals(existing.getOauthId())) {
			throw new BusinessException(ErrorCode.CONFLICT, "이미 현재 계정에 동일 소셜 계정이 연결되어 있습니다.");
		}

		dao.transferOAuthUser(provider, providerUserId, toUserId);
	}

}
