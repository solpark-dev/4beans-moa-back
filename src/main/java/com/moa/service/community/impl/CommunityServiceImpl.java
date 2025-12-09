package com.moa.service.community.impl;

import com.moa.domain.enums.PushCodeType;
import com.moa.dao.community.CommunityDao;
import com.moa.domain.Community;
import com.moa.dto.community.request.*;
import com.moa.dto.community.response.*;
import com.moa.dto.push.request.TemplatePushRequest;
import com.moa.service.community.CommunityService;
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
public class CommunityServiceImpl implements CommunityService {
    
    private final CommunityDao communityDao;
    private final PushService pushService;
    
    @Override
    public PageResponse<NoticeResponse> getNoticeList(int page, int size) {
        int offset = (page - 1) * size;
        List<Community> noticeList = communityDao.getNoticeList(offset, size);
        int totalCount = communityDao.getNoticeTotalCount();
        
        List<NoticeResponse> responseList = noticeList.stream()
                .map(NoticeResponse::fromEntity)
                .collect(Collectors.toList());
        
        return new PageResponse<>(responseList, page, size, totalCount);
    }
    
    @Override
    @Transactional
    public NoticeResponse getNotice(Integer communityId) {
        communityDao.incrementViewCount(communityId);  //조회수 증가 추가
        Community community = communityDao.getNotice(communityId);
        return NoticeResponse.fromEntity(community);
    }
    
    @Override
    @Transactional
    public void addNotice(NoticeRequest request) {
        Community community = request.toEntity();
        communityDao.addNotice(community);
    }
    
    @Override
    @Transactional
    public void updateNotice(Integer communityId, NoticeRequest request) {
        Community community = request.toEntity();
        community.setCommunityId(communityId);
        communityDao.updateNotice(community);
    }
    
    @Override
    public PageResponse<NoticeResponse> searchNotice(String keyword, int page, int size) {
        int offset = (page - 1) * size;
        List<Community> noticeList = communityDao.searchNotice(keyword, offset, size);
        int totalCount = communityDao.searchNoticeTotalCount(keyword);
        
        List<NoticeResponse> responseList = noticeList.stream()
                .map(NoticeResponse::fromEntity)
                .collect(Collectors.toList());
        
        return new PageResponse<>(responseList, page, size, totalCount);
    }
    
    @Override
    public PageResponse<FaqResponse> getFaqList(int page, int size) {
        int offset = (page - 1) * size;
        List<Community> faqList = communityDao.getFaqList(offset, size);
        int totalCount = communityDao.getFaqTotalCount();
        
        List<FaqResponse> responseList = faqList.stream()
                .map(FaqResponse::fromEntity)
                .collect(Collectors.toList());
        
        return new PageResponse<>(responseList, page, size, totalCount);
    }
    
    @Override
    public FaqResponse getFaq(Integer communityId) {
        Community community = communityDao.getFaq(communityId);
        return FaqResponse.fromEntity(community);
    }
    
    @Override
    @Transactional
    public void addFaq(FaqRequest request) {
        Community community = request.toEntity();
        communityDao.addFaq(community);
    }
    
    @Override
    @Transactional
    public void updateFaq(Integer communityId, FaqRequest request) {
        Community community = request.toEntity();
        community.setCommunityId(communityId);
        communityDao.updateFaq(community);
    }
    
    @Override
    public PageResponse<FaqResponse> searchFaq(String keyword, int page, int size) {
        int offset = (page - 1) * size;
        List<Community> faqList = communityDao.searchFaq(keyword, offset, size);
        int totalCount = communityDao.searchFaqTotalCount(keyword);
        
        List<FaqResponse> responseList = faqList.stream()
                .map(FaqResponse::fromEntity)
                .collect(Collectors.toList());
        
        return new PageResponse<>(responseList, page, size, totalCount);
    }
    
    @Override
    public PageResponse<InquiryResponse> getMyInquiryList(String userId, int page, int size) {
        int offset = (page - 1) * size;
        List<Community> inquiryList = communityDao.getMyInquiryList(userId, offset, size);
        int totalCount = communityDao.getMyInquiryTotalCount(userId);
        
        List<InquiryResponse> responseList = inquiryList.stream()
                .map(InquiryResponse::fromEntity)
                .collect(Collectors.toList());
        
        return new PageResponse<>(responseList, page, size, totalCount);
    }
    
    @Override
    public PageResponse<InquiryResponse> getInquiryList(int page, int size) {
        int offset = (page - 1) * size;
        List<Community> inquiryList = communityDao.getInquiryList(offset, size);
        int totalCount = communityDao.getInquiryTotalCount();
        
        List<InquiryResponse> responseList = inquiryList.stream()
                .map(InquiryResponse::fromEntity)
                .collect(Collectors.toList());
        
        return new PageResponse<>(responseList, page, size, totalCount);
    }
    
    @Override
    public InquiryResponse getInquiry(Integer communityId) {
        Community community = communityDao.getInquiry(communityId);
        return InquiryResponse.fromEntity(community);
    }
    
    @Override
    @Transactional
    public void addInquiry(InquiryRequest request) {
        Community community = request.toEntity();
        communityDao.addInquiry(community);
    }
    
    @Override
    @Transactional
    public void addAnswer(AnswerRequest request) {
        communityDao.addAnswer(request.getCommunityId(), request.getAnswerContent());
        
        Community inquiry = communityDao.getInquiry(request.getCommunityId());
        
        TemplatePushRequest pushRequest = TemplatePushRequest.builder()
                .receiverId(inquiry.getUserId())
                .pushCode(PushCodeType.INQUIRY_ANSWER.getCode())
                .params(Map.of("nickname", inquiry.getNickname()))
                .moduleId(String.valueOf(request.getCommunityId()))
                .moduleType(PushCodeType.INQUIRY_ANSWER.getModuleType())
                .build();
        
        pushService.addTemplatePush(pushRequest);
    }
    
    
    
    @Override
    @Transactional
    public void updateAnswer(AnswerRequest request) {
        communityDao.updateAnswer(request.getCommunityId(), request.getAnswerContent());
    }
}