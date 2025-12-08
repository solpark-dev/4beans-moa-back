package com.moa.service.push;

import com.moa.dto.community.response.PageResponse;
import com.moa.dto.push.request.PushRequest;
import com.moa.dto.push.request.TemplatePushRequest;
import com.moa.dto.push.response.PushResponse;

public interface PushService {
    
    void sendPush(PushRequest request);
    
    void sendTemplatePush(TemplatePushRequest request);
    
    PageResponse<PushResponse> getPushList(String receiverId, int page, int size);
    
    PushResponse getPush(Integer pushId);
    
    void markAsRead(Integer pushId);
    
    void markAsDeleted(Integer pushId);
    
    int getUnreadCount(String receiverId);
}