package com.moa.dao.push;

import com.moa.domain.Push;
import com.moa.domain.PushCode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface PushDao {

    int addPush(Push push);

    PushCode getPushCodeByName(@Param("codeName") String codeName);

    List<Push> getPushList(@Param("offset") int offset, @Param("limit") int limit);

    int getPushTotalCount();

    List<Push> getMyPushList(@Param("receiverId") String receiverId, @Param("offset") int offset, @Param("limit") int limit);

    int getMyPushTotalCount(@Param("receiverId") String receiverId);

    Push getPush(@Param("pushId") Integer pushId);

    int updateRead(@Param("pushId") Integer pushId);

    int updateAllRead(@Param("receiverId") String receiverId);

    int deletePush(@Param("pushId") Integer pushId);

    int deleteAllPushs(@Param("receiverId") String receiverId);

    int getUnreadCount(@Param("receiverId") String receiverId);

    List<PushCode> getPushCodeList();

    PushCode getPushCodeById(@Param("pushCodeId") Integer pushCodeId);

    int addPushCode(PushCode pushCode);

    int updatePushCode(@Param("pushCodeId") Integer pushCodeId,
                       @Param("codeName") String codeName,
                       @Param("titleTemplate") String titleTemplate,
                       @Param("contentTemplate") String contentTemplate);

    int deletePushCode(@Param("pushCodeId") Integer pushCodeId);

    List<Push> getPushHistory(@Param("offset") int offset,
                              @Param("limit") int limit,
                              @Param("pushCode") String pushCode,
                              @Param("receiverId") String receiverId,
                              @Param("startDate") String startDate,
                              @Param("endDate") String endDate);

    int getPushHistoryCount(@Param("pushCode") String pushCode,
                            @Param("receiverId") String receiverId,
                            @Param("startDate") String startDate,
                            @Param("endDate") String endDate);

    List<Map<String, String>> searchUsersForPush(@Param("keyword") String keyword);

    List<String> getAllUserIdsExceptAdmin();
}