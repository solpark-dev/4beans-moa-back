package com.moa.dao.push;

import com.moa.domain.Push;
import com.moa.domain.PushCode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PushDao {

    int addPush(Push push);

    PushCode getPushCodeByName(@Param("codeName") String codeName);

    List<Push> getPushList(@Param("receiverId") String receiverId, @Param("offset") int offset, @Param("limit") int limit);

    int getPushTotalCount(@Param("receiverId") String receiverId);

    Push getPush(@Param("pushId") Integer pushId);

    int markAsRead(@Param("pushId") Integer pushId);

    int markAsDeleted(@Param("pushId") Integer pushId);

    int getUnreadCount(@Param("receiverId") String receiverId);
}