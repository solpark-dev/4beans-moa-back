package com.moa.service.blacklist.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moa.common.exception.BusinessException;
import com.moa.common.exception.ErrorCode;
import com.moa.dao.admin.AdminDao;
import com.moa.dao.user.UserDao;
import com.moa.domain.enums.UserStatus;
import com.moa.dto.blacklist.AddBlacklistRequest;
import com.moa.dto.blacklist.DeleteBlacklistRequest;
import com.moa.service.blacklist.BlacklistService;

@Service
@Transactional
public class BlacklistServiceImpl implements BlacklistService {

	private final AdminDao adminDao;
	private final UserDao userDao;

	public BlacklistServiceImpl(AdminDao adminDao, UserDao userDao) {
		this.adminDao = adminDao;
		this.userDao = userDao;
	}

	@Override
	public void addBlacklist(AddBlacklistRequest request) {
		String userId = request.getUserId();

		userDao.findByUserId(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		String reason = request.getReasonType();
		if (request.getReasonDetail() != null && !request.getReasonDetail().isBlank()) {
			reason = reason + " - " + request.getReasonDetail();
		}

		adminDao.insertBlacklist(userId, reason);

		int updated = userDao.updateUserStatus(userId, UserStatus.BLOCK);
		if (updated == 0) {
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "회원 상태 변경에 실패했습니다.");
		}
	}

	@Override
	public void deleteBlacklist(DeleteBlacklistRequest request) {
		String userId = request.getUserId();

		userDao.findByUserId(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		adminDao.deactivateBlacklist(userId, request.getDeleteReason());

		int updated = userDao.updateUserStatus(userId, UserStatus.ACTIVE);
		if (updated == 0) {
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "회원 상태 복구에 실패했습니다.");
		}
	}
}
