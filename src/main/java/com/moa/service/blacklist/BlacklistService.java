package com.moa.service.blacklist;

import com.moa.dto.blacklist.AddBlacklistRequest;
import com.moa.dto.blacklist.DeleteBlacklistRequest;

public interface BlacklistService {

	void addBlacklist(AddBlacklistRequest request);

	void deleteBlacklist(DeleteBlacklistRequest request);
}