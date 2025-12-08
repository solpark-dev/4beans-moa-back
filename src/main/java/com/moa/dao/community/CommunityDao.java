package com.moa.dao.community;

import com.moa.domain.Community;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface CommunityDao {

    List<Community> getNoticeList(@Param("offset") int offset, @Param("limit") int limit);

    int getNoticeTotalCount();

    Community getNotice(@Param("communityId") Integer communityId);

    int addNotice(Community community);

    int updateNotice(Community community);
    
    int deleteNotice(@Param("communityId") Integer communityId);

    int incrementViewCount(@Param("communityId") Integer communityId);

    List<Community> searchNotice(@Param("keyword") String keyword, @Param("offset") int offset, @Param("limit") int limit);

    int searchNoticeTotalCount(@Param("keyword") String keyword);

    List<Community> getFaqList(@Param("offset") int offset, @Param("limit") int limit);

    int getFaqTotalCount();

    Community getFaq(@Param("communityId") Integer communityId);

    int addFaq(Community community);

    int updateFaq(Community community);
    
    int deleteFaq(@Param("communityId") Integer communityId);

    List<Community> searchFaq(@Param("keyword") String keyword, @Param("offset") int offset, @Param("limit") int limit);

    int searchFaqTotalCount(@Param("keyword") String keyword);

    List<Community> getMyInquiryList(@Param("userId") String userId, @Param("offset") int offset, @Param("limit") int limit);

    int getMyInquiryTotalCount(@Param("userId") String userId);

    List<Community> getInquiryList(@Param("offset") int offset, @Param("limit") int limit);

    int getInquiryTotalCount();

    Community getInquiry(@Param("communityId") Integer communityId);

    int addInquiry(Community community);
    
    int deleteInquiry(@Param("communityId") Integer communityId);

    // 답변 등록/수정 - @Param 방식으로 변경
    int addAnswer(@Param("communityId") Integer communityId, @Param("answerContent") String answerContent);

    int updateAnswer(@Param("communityId") Integer communityId, @Param("answerContent") String answerContent);
}