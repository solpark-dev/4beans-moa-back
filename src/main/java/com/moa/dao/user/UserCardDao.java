package com.moa.dao.user;

import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.moa.domain.UserCard;

@Mapper
public interface UserCardDao {
    int insertUserCard(UserCard userCard);

    Optional<UserCard> findByUserId(@Param("userId") String userId);

    int deleteUserCard(@Param("userId") String userId);
}
