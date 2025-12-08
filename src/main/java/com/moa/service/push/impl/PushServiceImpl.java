package com.moa.service.push.impl;

import com.moa.dao.push.PushDao;
import com.moa.domain.Push;
import com.moa.domain.PushCode;
import com.moa.dto.community.response.PageResponse;
import com.moa.dto.push.request.PushRequest;
import com.moa.dto.push.request.TemplatePushRequest;
import com.moa.dto.push.response.PushResponse;
import com.moa.service.push.PushService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PushServiceImpl implements PushService {
    
    private final PushDao pushDao;
    
    @Override
    @Transactional
    public void sendPush(PushRequest request) {
        Push push = request.toEntity();
        pushDao.addPush(push);
    }
    
    @Override
    @Transactional
    public void sendTemplatePush(TemplatePushRequest request) {
        PushCode pushCode = pushDao.getPushCodeByName(request.getPushCode());
        
        if (pushCode == null) {
            throw new IllegalArgumentException("Invalid push code: " + request.getPushCode());
        }
        
        String title = replaceTemplateParams(pushCode.getTitleTemplate(), request.getParams());
        String content = replaceTemplateParams(pushCode.getContentTemplate(), request.getParams());
        
        Push push = Push.builder()
                .receiverId(request.getReceiverId())
                .pushCode(request.getPushCode())
                .title(title)
                .content(content)
                .moduleId(request.getModuleId())
                .moduleType(request.getModuleType())
                .build();
        
        pushDao.addPush(push);
    }
    
    @Override
    public PageResponse<PushResponse> getPushList(String receiverId, int page, int size) {
        int offset = (page - 1) * size;
        List<Push> pushList = pushDao.getPushList(receiverId, offset, size);
        int totalCount = pushDao.getPushTotalCount(receiverId);
        
        List<PushResponse> responseList = pushList.stream()
                .map(PushResponse::fromEntity)
                .collect(Collectors.toList());
        
        return new PageResponse<>(responseList, page, size, totalCount);
    }
    
    @Override
    public PushResponse getPush(Integer pushId) {
        Push push = pushDao.getPush(pushId);
        return PushResponse.fromEntity(push);
    }
    
    @Override
    @Transactional
    public void markAsRead(Integer pushId) {
        pushDao.markAsRead(pushId);
    }
    
    @Override
    @Transactional
    public void markAsDeleted(Integer pushId) {
        pushDao.markAsDeleted(pushId);
    }
    
    @Override
    public int getUnreadCount(String receiverId) {
        return pushDao.getUnreadCount(receiverId);
    }
    
    private String replaceTemplateParams(String template, Map<String, String> params) {
        String result = template;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}