package com.moa.dao.oauth;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.moa.domain.OAuthAccount;

@Mapper
public interface OAuthAccountDao {

	OAuthAccount getOAuthAccount(String oauthId);

	List<OAuthAccount> getOAuthAccountList(String userId);

	OAuthAccount getOAuthByProvider(@Param("provider") String provider, @Param("providerUserId") String providerUserId);

	int addOAuthAccount(OAuthAccount account);

	int updateOAuthRelease(String oauthId);

	int reconnectOAuthAccount(String oauthId);

	OAuthAccount getOAuthByUserAndProvider(@Param("userId") String userId, @Param("provider") String provider);

	int transferOAuthUser(@Param("provider") String provider, @Param("providerUserId") String providerUserId,
			@Param("userId") String userId);
}
