package com.moa.service.user;

import java.util.Optional;

import org.apache.ibatis.annotations.Param;
import org.springframework.web.multipart.MultipartFile;

import com.moa.domain.User;
import com.moa.dto.community.response.PageResponse;
import com.moa.dto.user.request.AdminUserSearchRequest;
import com.moa.dto.user.request.CommonCheckRequest;
import com.moa.dto.user.request.DeleteUserRequest;
import com.moa.dto.user.request.PasswordResetRequest;
import com.moa.dto.user.request.PasswordResetStartRequest;
import com.moa.dto.user.request.PasswordUpdateRequest;
import com.moa.dto.user.request.UserCreateRequest;
import com.moa.dto.user.request.UserUpdateRequest;
import com.moa.dto.user.response.AdminUserListItemResponse;
import com.moa.dto.user.response.CommonCheckResponse;
import com.moa.dto.user.response.PasswordResetTokenResponse;
import com.moa.dto.user.response.UserResponse;

public interface UserService {

	CommonCheckResponse check(CommonCheckRequest request);

	void validatePasswordRule(String password);

	void validatePasswordConfirm(String password, String passwordConfirm);

	PasswordResetTokenResponse startPasswordReset(PasswordResetStartRequest request);

	void resetPassword(PasswordResetRequest request);

	void updatePassword(String userId, PasswordUpdateRequest request);

	void checkCurrentPassword(String userId, String currentPassword);

	UserResponse addUser(UserCreateRequest request);

	UserResponse getCurrentUser();

	User findUserIncludeDeleted(String userId);

	UserResponse updateUser(String userId, UserUpdateRequest request);

	String uploadProfileImage(String userId, MultipartFile file);

	void deleteCurrentUser(String userId, DeleteUserRequest request);

	void restoreUser(@Param("userId") String userId);

	String findUserIdByPhone(String phone);

	PageResponse<AdminUserListItemResponse> getAdminUserList(AdminUserSearchRequest request);

	UserResponse getUserDetailForAdmin(String userId);
	
	void unlockByCertification(String userId, String phone, String ci);
	
	Optional<User> findByPhone(String phone);
}
