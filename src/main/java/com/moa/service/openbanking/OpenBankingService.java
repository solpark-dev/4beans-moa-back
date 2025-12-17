package com.moa.service.openbanking;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.moa.common.exception.BusinessException;
import com.moa.common.exception.ErrorCode;
import com.moa.config.OpenBankingConfig;
import com.moa.dao.account.AccountDao;
import com.moa.domain.Account;
import com.moa.domain.enums.PushCodeType;
import com.moa.dto.push.request.TemplatePushRequest;
import com.moa.service.push.PushService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenBankingService {

	private final OpenBankingConfig config;
	private final RestTemplate restTemplate;
	private final AccountDao accountDao;
	private final PushService pushService;
	private final Map<String, String> verificationCodes = new HashMap<>();
	private final Map<String, String> userTokens = new HashMap<>();
	private final Map<String, Integer> verificationAttempts = new HashMap<>();
	private static final int MAX_VERIFY_ATTEMPTS = 3;

	public String getAuthorizationUrl(String userId) {
		String baseUrl = config.getApiUrl() + "/oauth/2.0/authorize";

		try {
			String encodedRedirectUri = java.net.URLEncoder.encode(config.getCallbackUrl(), "UTF-8");
			String encodedScope = java.net.URLEncoder.encode("login inquiry transfer", "UTF-8");
			String authUrl = baseUrl + "?" + "response_type=code" + "&client_id=" + config.getClientId()
					+ "&redirect_uri=" + encodedRedirectUri + "&scope=" + encodedScope + "&state=" + userId
					+ "&auth_type=0";

			log.info("=== OpenBanking Auth URL Generation ===");
			log.info("Base URL: {}", baseUrl);
			log.info("Client ID: {}", config.getClientId());
			log.info("Redirect URI (original): {}", config.getCallbackUrl());
			log.info("Redirect URI (encoded): {}", encodedRedirectUri);
			log.info("Scope (encoded): {}", encodedScope);
			log.info("State (userId): {}", userId);
			log.info("Full Auth URL: {}", authUrl);
			log.info("=======================================");

			return authUrl;
		} catch (java.io.UnsupportedEncodingException e) {
			log.error("URL encoding failed", e);
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "인증 URL 생성에 실패했습니다.");
		}
	}

	public void processCallback(String userId, String code) {
		String accessToken = requestUserToken(code);
		userTokens.put(userId, accessToken);
		JsonNode userInfo = getUserInfo(accessToken);
		JsonNode accountList = userInfo.get("res_list");
		if (accountList != null && accountList.isArray() && accountList.size() > 0) {
			JsonNode firstAccount = accountList.get(0);

			Account account = Account.builder().userId(userId).bankCode(firstAccount.get("bank_code_std").asText())
					.bankName(getBankName(firstAccount.get("bank_code_std").asText()))
					.accountNumber(firstAccount.get("account_num_masked").asText())
					.accountHolder(firstAccount.get("account_holder_name").asText()).isVerified("N")
					.regDate(LocalDateTime.now()).build();
			accountDao.findByUserId(userId)
					.ifPresent(existing -> accountDao.updateVerifyStatus(existing.getAccountId(), "N"));

			accountDao.insertAccount(account);
			log.info("Account registered for user: {}", userId);
		}
	}

	private String requestUserToken(String code) {
		String url = config.getApiUrl() + "/oauth/2.0/token";

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("client_id", config.getClientId());
		params.add("client_secret", config.getClientSecret());
		params.add("code", code);
		params.add("redirect_uri", config.getCallbackUrl());
		params.add("grant_type", "authorization_code");

		try {
			HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);
			ResponseEntity<JsonNode> response = restTemplate.postForEntity(url, entity, JsonNode.class);
			JsonNode body = response.getBody();

			if (body != null && body.has("access_token")) {
				return body.get("access_token").asText();
			} else {
				log.error("Token request failed: {}", body);
				throw new BusinessException(ErrorCode.INTERNAL_ERROR, "토큰 발급에 실패했습니다.");
			}
		} catch (Exception e) {
			log.error("Token request error", e);
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "오픈뱅킹 인증에 실패했습니다.");
		}
	}

	private JsonNode getUserInfo(String accessToken) {
		String url = config.getApiUrl() + "/v2.0/user/me";

		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);
		headers.setContentType(MediaType.APPLICATION_JSON);

		try {
			HttpEntity<String> entity = new HttpEntity<>(headers);
			ResponseEntity<JsonNode> response = restTemplate.postForEntity(url, entity, JsonNode.class);
			return response.getBody();
		} catch (Exception e) {
			log.error("User info request error", e);
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "계좌 정보 조회에 실패했습니다.");
		}
	}

	public String sendOneWonDeposit(String userId) {
		Account account = accountDao.findByUserId(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "등록된 계좌가 없습니다."));
		String verificationCode = String.format("%04d", new Random().nextInt(10000));
		verificationCodes.put(userId, verificationCode);
		verificationAttempts.put(userId, 0);
		String depositorName = "MOA" + verificationCode;

		try {
			depositToUser(account.getBankCode(), account.getAccountNumber(), account.getAccountHolder(), 1,
					depositorName);
			log.info("1원 인증 입금 완료 - userId: {}, code: {}", userId, verificationCode);
			sendVerifyRequestedPush(userId, account);

			return verificationCode;
		} catch (Exception e) {
			log.error("1원 입금 실패", e);
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "1원 입금에 실패했습니다.");
		}
	}

	public boolean verifyAccount(String userId, String inputCode) {
		String storedCode = verificationCodes.get(userId);

		int attemptCount = verificationAttempts.getOrDefault(userId, 0) + 1;
		verificationAttempts.put(userId, attemptCount);

		if (storedCode != null && storedCode.equals(inputCode)) {
			Account account = accountDao.findByUserId(userId)
					.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "등록된 계좌가 없습니다."));

			accountDao.updateVerifyStatus(account.getAccountId(), "Y");
			verificationCodes.remove(userId);
			verificationAttempts.remove(userId);

			log.info("계좌 인증 완료 - userId: {}", userId);
			sendAccountVerifiedPush(userId, account);

			return true;
		}
		if (attemptCount >= MAX_VERIFY_ATTEMPTS) {
			verificationCodes.remove(userId);
			verificationAttempts.remove(userId);
			sendVerifyExceededPush(userId);
			log.warn("계좌 인증 시도 횟수 초과 - userId: {}", userId);
		}

		return false;
	}

	public String depositToUser(String bankCode, String accountNumber, String accountHolderName, Integer amount,
			String printContent) {
		String accessToken = requestAdminToken();
		String url = config.getApiUrl() + "/v2.0/transfer/deposit/acnt_num";

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBearerAuth(accessToken);

		Map<String, Object> body = new HashMap<>();
		String bankTranId = generateBankTranId();

		body.put("cntr_account_type", "N");
		body.put("cntr_account_num", config.getPlatformAccountNumber());
		body.put("wd_pass_phrase", "NONE");
		body.put("wd_print_content", printContent);
		body.put("name_check_option", "on");
		body.put("tran_dtime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
		body.put("req_cnt", "1");

		Map<String, Object> reqItem = new HashMap<>();
		reqItem.put("tran_no", "1");
		reqItem.put("bank_tran_id", bankTranId);
		reqItem.put("bank_code_std", bankCode);
		reqItem.put("account_num", accountNumber);
		reqItem.put("account_holder_name", accountHolderName);
		reqItem.put("print_content", printContent);
		reqItem.put("tran_amt", String.valueOf(amount));
		reqItem.put("req_client_name", config.getPlatformAccountHolder());
		reqItem.put("req_client_bank_code", config.getPlatformBankCode());
		reqItem.put("req_client_account_num", config.getPlatformAccountNumber());
		reqItem.put("req_client_num", config.getPlatformClientNum());
		reqItem.put("transfer_purpose", "TR");

		body.put("req_list", Collections.singletonList(reqItem));

		try {
			HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
			ResponseEntity<JsonNode> response = restTemplate.postForEntity(url, entity, JsonNode.class);
			JsonNode resBody = response.getBody();

			if (resBody != null && resBody.has("rsp_code") && "A0000".equals(resBody.get("rsp_code").asText())) {
				log.info("Deposit transfer completed successfully");
				JsonNode resList = resBody.get("res_list");
				if (resList != null && resList.isArray() && resList.size() > 0) {
					return resList.get(0).get("bank_tran_id").asText();
				}
				return bankTranId;
			} else {
				String errorCode = resBody != null && resBody.has("rsp_code") ? resBody.get("rsp_code").asText()
						: "UNKNOWN";
				String errorMessage = resBody != null && resBody.has("rsp_message")
						? resBody.get("rsp_message").asText()
						: "No error message";
				log.error("Deposit transfer failed - Error code: {}, Message: {}", errorCode, errorMessage);
				throw new BusinessException(ErrorCode.INTERNAL_ERROR);
			}

		} catch (Exception e) {
			log.error("OpenBanking API error", e);
			throw new BusinessException(ErrorCode.INTERNAL_ERROR);
		}
	}

	public String depositToUser(String bankCode, String accountNumber, String accountHolderName, Integer amount) {
		return depositToUser(bankCode, accountNumber, accountHolderName, amount, "MOA정산");
	}

	private String requestAdminToken() {
		String url = config.getApiUrl() + "/oauth/2.0/token";

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("client_id", config.getClientId());
		params.add("client_secret", config.getClientSecret());
		params.add("scope", "oob");
		params.add("grant_type", "client_credentials");

		try {
			HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);
			ResponseEntity<JsonNode> response = restTemplate.postForEntity(url, entity, JsonNode.class);
			JsonNode body = response.getBody();

			if (body != null && body.has("access_token")) {
				return body.get("access_token").asText();
			} else {
				throw new BusinessException(ErrorCode.INTERNAL_ERROR);
			}
		} catch (Exception e) {
			log.error("Token request failed", e);
			throw new BusinessException(ErrorCode.INTERNAL_ERROR);
		}
	}

	private String generateBankTranId() {
		String institutionCode = config.getInstitutionCode();
		String randomStr = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 9).toUpperCase();
		return institutionCode + randomStr + "U";
	}

	private String getBankName(String bankCode) {
		Map<String, String> bankNames = Map.ofEntries(Map.entry("004", "국민은행"), Map.entry("020", "우리은행"),
				Map.entry("088", "신한은행"), Map.entry("003", "기업은행"), Map.entry("011", "농협은행"), Map.entry("081", "하나은행"),
				Map.entry("023", "SC제일은행"), Map.entry("039", "경남은행"), Map.entry("034", "광주은행"),
				Map.entry("031", "대구은행"), Map.entry("032", "부산은행"), Map.entry("037", "전북은행"), Map.entry("035", "제주은행"),
				Map.entry("090", "카카오뱅크"), Map.entry("089", "케이뱅크"), Map.entry("092", "토스뱅크"));
		return bankNames.getOrDefault(bankCode, "기타은행");
	}

	private void sendVerifyRequestedPush(String userId, Account account) {
		try {
			Map<String, String> params = Map.of("bankName",
					account.getBankName() != null ? account.getBankName() : "은행", "accountNumber",
					maskAccountNumber(account.getAccountNumber()));

			TemplatePushRequest pushRequest = TemplatePushRequest.builder().receiverId(userId)
					.pushCode(PushCodeType.VERIFY_REQUESTED.getCode()).params(params)
					.moduleId(String.valueOf(account.getAccountId()))
					.moduleType(PushCodeType.VERIFY_REQUESTED.getModuleType()).build();

			pushService.addTemplatePush(pushRequest);
			log.info("푸시알림 발송 완료: VERIFY_REQUESTED -> userId={}", userId);

		} catch (Exception e) {
			log.error("푸시알림 발송 실패: userId={}, error={}", userId, e.getMessage());
		}
	}

	private void sendAccountVerifiedPush(String userId, Account account) {
		try {
			Map<String, String> params = Map.of("bankName",
					account.getBankName() != null ? account.getBankName() : "은행", "accountNumber",
					maskAccountNumber(account.getAccountNumber()));

			TemplatePushRequest pushRequest = TemplatePushRequest.builder().receiverId(userId)
					.pushCode(PushCodeType.ACCOUNT_VERIFIED.getCode()).params(params)
					.moduleId(String.valueOf(account.getAccountId()))
					.moduleType(PushCodeType.ACCOUNT_VERIFIED.getModuleType()).build();

			pushService.addTemplatePush(pushRequest);
			log.info("푸시알림 발송 완료: ACCOUNT_VERIFIED -> userId={}", userId);

		} catch (Exception e) {
			log.error("푸시알림 발송 실패: userId={}, error={}", userId, e.getMessage());
		}
	}

	private void sendVerifyExceededPush(String userId) {
		try {
			Map<String, String> params = Map.of("maxAttempts", String.valueOf(MAX_VERIFY_ATTEMPTS));

			TemplatePushRequest pushRequest = TemplatePushRequest.builder().receiverId(userId)
					.pushCode(PushCodeType.VERIFY_EXCEEDED.getCode()).params(params).moduleId(userId)
					.moduleType(PushCodeType.VERIFY_EXCEEDED.getModuleType()).build();

			pushService.addTemplatePush(pushRequest);
			log.info("푸시알림 발송 완료: VERIFY_EXCEEDED -> userId={}", userId);

		} catch (Exception e) {
			log.error("푸시알림 발송 실패: userId={}, error={}", userId, e.getMessage());
		}
	}

	private String maskAccountNumber(String accountNumber) {
		if (accountNumber == null || accountNumber.length() < 4) {
			return "****";
		}
		return "****" + accountNumber.substring(accountNumber.length() - 4);
	}
}