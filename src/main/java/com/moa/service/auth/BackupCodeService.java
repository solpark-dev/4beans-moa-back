package com.moa.service.auth;

import java.util.List;

public interface BackupCodeService {

	List<String> issueForCurrentUser();

	void verifyForLogin(String userId, String code);

	boolean hasBackupCodesForCurrentUser();
}
