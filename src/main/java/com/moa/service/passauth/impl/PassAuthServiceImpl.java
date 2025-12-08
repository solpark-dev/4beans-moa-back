package com.moa.service.passauth.impl;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.moa.service.passauth.PassAuthService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PassAuthServiceImpl implements PassAuthService {

	private final RestTemplate restTemplate = new RestTemplate();

	@Value("${portone.imp-key}")
	private String apiKey;

	@Value("${portone.imp-secret}")
	private String apiSecret;

	@Value("${portone.imp-code}")
	private String impCode;

	@Override
	public Map<String, Object> requestCertification() {
		String merchantUid = "pass_" + System.currentTimeMillis();

		Map<String, Object> data = new HashMap<>();
		data.put("merchantUid", merchantUid);
		data.put("impCode", impCode);

		// Controller에서 ApiResponse로 감싸므로 여기서는 데이터만 반환
		return data;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<String, Object> verifyCertification(String impUid) throws Exception {
		String token = getAccessToken();

		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", "Bearer " + token);

		HttpEntity<?> req = new HttpEntity<>(headers);

		ResponseEntity<Map> res = restTemplate.exchange("https://api.iamport.kr/certifications/" + impUid,
				HttpMethod.GET, req, Map.class);

		// PortOne API 응답 파싱
		Map<String, Object> body = (Map<String, Object>) res.getBody();
		Map<String, Object> response = (Map<String, Object>) body.get("response");

		Map<String, Object> customer = null;
		if (response.containsKey("verifiedCustomer")) {
			customer = (Map<String, Object>) response.get("verifiedCustomer");
		}

		// 프론트엔드로 보낼 순수 데이터 구성
		Map<String, Object> data = new HashMap<>();
		data.put("phone", response.get("phone"));

		if (customer != null) {
			data.put("name", customer.get("name"));
			data.put("ci", customer.get("ci"));
			data.put("di", customer.get("di"));
		} else {
			data.put("name", response.get("name")); // 이름 정보가 바로 있을 수도 있음
			data.put("ci", response.get("unique_key"));
			data.put("di", response.get("unique_in_site"));
		}

		// [중요] 여기서 success: true 등으로 감싸지 않고 data 맵 자체를 반환함
		return data;
	}

	@SuppressWarnings("unchecked")
	private String getAccessToken() {
		Map<String, String> body = new HashMap<>();
		body.put("imp_key", apiKey);
		body.put("imp_secret", apiSecret);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		HttpEntity<Map<String, String>> req = new HttpEntity<>(body, headers);

		ResponseEntity<Map> res = restTemplate.exchange("https://api.iamport.kr/users/getToken", HttpMethod.POST, req,
				Map.class);

		Map<String, Object> response = (Map<String, Object>) res.getBody().get("response");
		return (String) response.get("access_token");
	}
}