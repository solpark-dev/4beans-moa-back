package com.moa.service.auth;

import com.moa.dto.community.response.PageResponse;
import com.moa.dto.user.response.LoginHistoryResponse;

public interface LoginHistoryService {

	void recordSuccess(String userId, String loginType, String loginIp, String userAgent);

	void recordFailure(String userId, String loginType, String loginIp, String userAgent, String failReason);

	PageResponse<LoginHistoryResponse> getMyLoginHistory(int page, int size);

	PageResponse<LoginHistoryResponse> getUserLoginHistory(String userId, int page, int size);
}
