package com.moa.dao.user;

import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.moa.domain.EmailVerification;

@Mapper
public interface EmailVerificationDao {

	int insert(EmailVerification emailVerification);

	Optional<EmailVerification> findByToken(@Param("token") String token);

	int updateVerifiedAt(@Param("token") String token);

	void expirePreviousTokens(@Param("userId") String userId);
}