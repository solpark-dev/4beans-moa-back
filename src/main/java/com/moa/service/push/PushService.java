package com.moa.service.push;

import com.moa.dto.community.response.PageResponse;
import com.moa.dto.push.request.AdminPushRequest;
import com.moa.dto.push.request.MultiPushRequest;
import com.moa.dto.push.request.PushCodeRequest;
import com.moa.dto.push.request.PushRequest;
import com.moa.dto.push.request.TemplatePushRequest;
import com.moa.dto.push.response.PushCodeResponse;
import com.moa.dto.push.response.PushResponse;

import java.util.List;
import java.util.Map;

public interface PushService {

    // ===== 기존 푸시 관련 =====

    void addPush(PushRequest request);

    void addPushMulti(MultiPushRequest request);

    void addTemplatePush(TemplatePushRequest request);

    PushResponse getPush(Integer pushId);

    PageResponse<PushResponse> getPushList(int page, int size);

    PageResponse<PushResponse> getMyPushList(String receiverId, int page, int size);

    int getUnreadCount(String receiverId);

    void updateRead(Integer pushId);

    void updateAllRead(String receiverId);

    void deletePush(Integer pushId);

    void deleteAllPushs(String receiverId);

    // ===== 관리자용: 푸시코드(템플릿) 관리 =====

    List<PushCodeResponse> getPushCodeList();

    PushCodeResponse getPushCode(Integer pushCodeId);

    void addPushCode(PushCodeRequest request);

    void updatePushCode(Integer pushCodeId, PushCodeRequest request);

    void deletePushCode(Integer pushCodeId);

    // ===== 관리자용: 발송 내역 조회 =====

    PageResponse<PushResponse> getPushHistory(int page, int size, String pushCode, String receiverId, String startDate, String endDate);

    // ===== 관리자용: 수동 발송 =====

    int sendAdminPush(AdminPushRequest request);

    List<Map<String, String>> searchUsersForPush(String keyword);
}