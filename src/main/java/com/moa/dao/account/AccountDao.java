package com.moa.dao.account;

import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.moa.domain.Account;

@Mapper
public interface AccountDao {
    int insertAccount(Account account);

    Optional<Account> findByUserId(@Param("userId") String userId);

    int updateVerifyStatus(@Param("accountId") Integer accountId, @Param("isVerified") String isVerified);

    Optional<Account> findById(@Param("accountId") Integer accountId);

    Optional<Account> findActiveByUserId(@Param("userId") String userId);

    int updateStatus(@Param("accountId") Integer accountId, @Param("status") String status);

    /**
     * 기존 계좌 정보 업데이트 (유저당 1개 계좌)
     */
    int updateAccountByUserId(Account account);

    int deleteByUserId(@Param("userId") String userId);
}
