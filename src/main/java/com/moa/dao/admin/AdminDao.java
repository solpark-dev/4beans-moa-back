package com.moa.dao.admin;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.moa.domain.Blacklist;
import com.moa.dto.user.request.AdminUserSearchRequest;
import com.moa.dto.user.response.AdminUserListItemResponse;

@Mapper
public interface AdminDao {

	long countAdminUsers(AdminUserSearchRequest request);

	List<AdminUserListItemResponse> findAdminUsers(AdminUserSearchRequest request);

	void insertBlacklist(@Param("userId") String userId, @Param("reason") String reason);

	Blacklist findActiveBlacklistByUserId(@Param("userId") String userId);

	int deactivateBlacklist(@Param("userId") String userId, @Param("deleteReason") String deleteReason);
}
