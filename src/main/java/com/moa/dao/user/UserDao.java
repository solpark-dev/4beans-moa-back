package com.moa.dao.user;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.moa.domain.User;
import com.moa.domain.enums.UserStatus;
import com.moa.dto.user.request.AdminUserSearchRequest;
import com.moa.dto.user.response.AdminUserListItemResponse;

@Mapper
public interface UserDao {

	Optional<User> findByUserIdIncludeDeleted(@Param("userId") String userId);

	int increaseLoginFailCount(@Param("userId") String userId);

	int getFailCount(@Param("userId") String userId);

	int blockUser(@Param("userId") String userId, @Param("unlockTime") LocalDateTime unlockTime);

	int resetLoginFailCount(@Param("userId") String userId);

	Optional<User> findByResetToken(@Param("token") String token);

	void savePasswordResetToken(@Param("userId") String userId, @Param("token") String token);

	void clearPasswordResetToken(@Param("userId") String userId);

	int insertUser(User user);

	Optional<User> findByUserId(@Param("userId") String userId);

	Optional<String> findUserIdByPhone(String phone);

	int existsByUserId(@Param("userId") String userId);

	int existsByNickname(@Param("nickname") String nickname);

	int existsByPhone(@Param("phone") String phone);

	int updateUserStatus(@Param("userId") String userId, @Param("status") UserStatus status);

	int updateLastLoginDate(@Param("userId") String userId);

	int updatePassword(@Param("userId") String userId, @Param("password") String password);

	int updateProfileImage(@Param("userId") String userId, @Param("imageUrl") String imageUrl);

	int updateUserProfile(User user);

	void softDeleteUser(@Param("userId") String userId, @Param("status") UserStatus status,
			@Param("deleteType") String deleteType, @Param("deleteDetail") String deleteDetail);

	void restoreUser(@Param("userId") String userId);

	List<User> findUserList(@Param("condition") String condition, @Param("q") String q, @Param("filter") String filter,
			@Param("offset") int offset, @Param("limit") int limit);

	long countUserList(@Param("condition") String condition, @Param("q") String q, @Param("filter") String filter);

}
