package com.moa.service.openbanking;

import com.moa.dto.openbanking.InquiryReceiveRequest;
import com.moa.dto.openbanking.InquiryReceiveResponse;
import com.moa.dto.openbanking.InquiryVerifyRequest;
import com.moa.dto.openbanking.InquiryVerifyResponse;
import com.moa.dto.openbanking.TransferDepositRequest;
import com.moa.dto.openbanking.TransferDepositResponse;

public interface OpenBankingClient {

	InquiryReceiveResponse requestVerification(InquiryReceiveRequest request);

	InquiryVerifyResponse verifyCode(InquiryVerifyRequest request);

	TransferDepositResponse transferDeposit(TransferDepositRequest request);
}
