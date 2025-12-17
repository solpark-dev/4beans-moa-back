package com.moa.service.account;

import com.moa.domain.Account;
import com.moa.dto.openbanking.InquiryReceiveResponse;
import com.moa.dto.openbanking.InquiryVerifyResponse;

public interface BankAccountService {

	InquiryReceiveResponse requestVerification(String userId, String bankCode, String accountNum, String accountHolder);

	InquiryVerifyResponse verifyAndRegister(String userId, String bankTranId, String verifyCode);

	Account getAccount(String userId);

	void deleteAccount(String userId);

	InquiryReceiveResponse changeAccount(String userId, String bankCode, String accountNum, String accountHolder);
}
