package com.moa.service.push.impl;

import com.moa.dao.push.PushDao;
import com.moa.domain.Push;
import com.moa.domain.PushCode;
import com.moa.dto.community.response.PageResponse;
import com.moa.dto.push.request.AdminPushRequest;
import com.moa.dto.push.request.MultiPushRequest;
import com.moa.dto.push.request.PushCodeRequest;
import com.moa.dto.push.request.PushRequest;
import com.moa.dto.push.request.TemplatePushRequest;
import com.moa.dto.push.response.PushCodeResponse;
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

    // ===== 기존 푸시 관련 =====

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

    // ===== 관리자용: 푸시코드(템플릿) 관리 =====

    @Override
    public List<PushCodeResponse> getPushCodeList() {
        List<PushCode> list = pushDao.getPushCodeList();
        return list.stream()
                .map(PushCodeResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public PushCodeResponse getPushCode(Integer pushCodeId) {
        PushCode pushCode = pushDao.getPushCodeById(pushCodeId);
        if (pushCode == null) {
            throw new IllegalArgumentException("존재하지 않는 푸시 코드입니다: " + pushCodeId);
        }
        return PushCodeResponse.fromEntity(pushCode);
    }

    @Override
    @Transactional
    public void addPushCode(PushCodeRequest request) {
        // 중복 체크
        PushCode existing = pushDao.getPushCodeByName(request.getCodeName());
        if (existing != null) {
            throw new IllegalArgumentException("이미 존재하는 푸시 코드입니다: " + request.getCodeName());
        }

        PushCode pushCode = request.toEntity();
        pushDao.addPushCode(pushCode);
    }

    @Override
    @Transactional
    public void updatePushCode(Integer pushCodeId, PushCodeRequest request) {
        // 존재 확인
        PushCode existing = pushDao.getPushCodeById(pushCodeId);
        if (existing == null) {
            throw new IllegalArgumentException("존재하지 않는 푸시 코드입니다: " + pushCodeId);
        }

        pushDao.updatePushCode(pushCodeId, request.getCodeName(), request.getTitleTemplate(), request.getContentTemplate());
    }

    @Override
    @Transactional
    public void deletePushCode(Integer pushCodeId) {
        // 존재 확인
        PushCode existing = pushDao.getPushCodeById(pushCodeId);
        if (existing == null) {
            throw new IllegalArgumentException("존재하지 않는 푸시 코드입니다: " + pushCodeId);
        }

        pushDao.deletePushCode(pushCodeId);
    }

    // ===== 관리자용: 발송 내역 조회 =====

    @Override
    public PageResponse<PushResponse> getPushHistory(int page, int size, String pushCode, String receiverId, String startDate, String endDate) {
        int offset = (page - 1) * size;
        List<Push> list = pushDao.getPushHistory(offset, size, pushCode, receiverId, startDate, endDate);
        int totalCount = pushDao.getPushHistoryCount(pushCode, receiverId, startDate, endDate);

        List<PushResponse> responseList = list.stream()
                .map(PushResponse::fromEntity)
                .collect(Collectors.toList());

        return new PageResponse<>(responseList, page, size, totalCount);
    }

    // ===== 관리자용: 수동 발송 =====

    @Override
    @Transactional
    public int sendAdminPush(AdminPushRequest request) {
        int count = 0;

        for (String receiverId : request.getReceiverIds()) {
            String title;
            String content;
            String pushCodeName;

            if ("TEMPLATE".equals(request.getSendType())) {
                // 템플릿 사용
                PushCode pushCode = pushDao.getPushCodeByName(request.getPushCode());
                if (pushCode == null) {
                    throw new IllegalArgumentException("존재하지 않는 푸시 코드입니다: " + request.getPushCode());
                }

                title = replaceTemplateParams(pushCode.getTitleTemplate(), request.getParams());
                content = replaceTemplateParams(pushCode.getContentTemplate(), request.getParams());
                pushCodeName = request.getPushCode();
            } else {
                // 직접 입력
                title = request.getTitle();
                content = request.getContent();
                pushCodeName = "ADMIN_CUSTOM";
            }

            Push push = Push.builder()
                    .receiverId(receiverId)
                    .pushCode(pushCodeName)
                    .title(title)
                    .content(content)
                    .moduleId(request.getModuleId())
                    .moduleType(request.getModuleType())
                    .build();

            pushDao.addPush(push);
            count++;
        }

        return count;
    }

    @Override
    public List<Map<String, String>> searchUsersForPush(String keyword) {
        return pushDao.searchUsersForPush(keyword);
    }

    // ===== Private Helper =====

    private String replaceTemplateParams(String template, Map<String, String> params) {
        if (template == null || params == null) {
            return template;
        }
        String result = template;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}