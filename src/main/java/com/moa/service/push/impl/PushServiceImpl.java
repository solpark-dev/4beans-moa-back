package com.moa.service.push.impl;

import com.moa.dao.push.PushDao;
import com.moa.domain.Push;
import com.moa.domain.PushCode;
import com.moa.dto.community.response.PageResponse;
import com.moa.dto.push.request.MultiPushRequest;
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
    public void addPush(PushRequest request) {
        Push push = request.toEntity();
        pushDao.addPush(push);
    }
    
    @Override
    @Transactional
    public void addPushMulti(MultiPushRequest request) {
        for (String receiverId : request.getReceiverIds()) {
            Push push = Push.builder()
                    .receiverId(receiverId)
                    .pushCode(request.getPushCode())
                    .title(request.getTitle())
                    .content(request.getContent())
                    .moduleId(request.getModuleId())
                    .moduleType(request.getModuleType())
                    .build();
            pushDao.addPush(push);
        }
    }
    
    @Override
    @Transactional
    public void addTemplatePush(TemplatePushRequest request) {
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
    public PushResponse getPush(Integer pushId) {
        Push push = pushDao.getPush(pushId);
        return PushResponse.fromEntity(push);
    }
    
    @Override
    public PageResponse<PushResponse> getPushList(int page, int size) {
        int offset = (page - 1) * size;
        List<Push> pushList = pushDao.getPushList(offset, size);
        int totalCount = pushDao.getPushTotalCount();
        
        List<PushResponse> responseList = pushList.stream()
                .map(PushResponse::fromEntity)
                .collect(Collectors.toList());
        
        return new PageResponse<>(responseList, page, size, totalCount);
    }
    
    @Override
    public PageResponse<PushResponse> getMyPushList(String receiverId, int page, int size) {
        int offset = (page - 1) * size;
        List<Push> pushList = pushDao.getMyPushList(receiverId, offset, size);
        int totalCount = pushDao.getMyPushTotalCount(receiverId);
        
        List<PushResponse> responseList = pushList.stream()
                .map(PushResponse::fromEntity)
                .collect(Collectors.toList());
        
        return new PageResponse<>(responseList, page, size, totalCount);
    }
    
    @Override
    public int getUnreadCount(String receiverId) {
        return pushDao.getUnreadCount(receiverId);
    }
    
    @Override
    @Transactional
    public void updateRead(Integer pushId) {
        pushDao.updateRead(pushId);
    }
    
    @Override
    @Transactional
    public void updateAllRead(String receiverId) {
        pushDao.updateAllRead(receiverId);
    }
    
    @Override
    @Transactional
    public void deletePush(Integer pushId) {
        pushDao.deletePush(pushId);
    }
    
    @Override
    @Transactional
    public void deleteAllPushs(String receiverId) {
        pushDao.deleteAllPushs(receiverId);
    }
    
    private String replaceTemplateParams(String template, Map<String, String> params) {
        String result = template;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}