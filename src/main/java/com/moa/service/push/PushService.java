package com.moa.service.push;

import com.moa.dto.community.response.PageResponse;
import com.moa.dto.push.request.MultiPushRequest;
import com.moa.dto.push.request.PushRequest;
import com.moa.dto.push.request.TemplatePushRequest;
import com.moa.dto.push.response.PushResponse;

public interface PushService {
    
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
}