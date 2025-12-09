package com.moa.dao.user;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.moa.domain.OtpBackupCode;

@Mapper
public interface OtpBackupCodeDao {

	void insert(@Param("userId") String userId, @Param("codeHash") String codeHash);

	List<OtpBackupCode> findValidCodes(@Param("userId") String userId);

	int markUsed(@Param("id") Long id);

	void deleteAllByUserId(@Param("userId") String userId);
}
