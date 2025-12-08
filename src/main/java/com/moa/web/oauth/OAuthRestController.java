package com.moa.web.oauth;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.moa.auth.provider.JwtProvider;
import com.moa.common.exception.ApiResponse;
import com.moa.common.exception.ErrorCode;
import com.moa.config.GoogleOAuthProperties;
import com.moa.config.KakaoOAuthProperties;
import com.moa.domain.OAuthAccount;
import com.moa.service.oauth.OAuthAccountService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/oauth")
@RequiredArgsConstructor
public class OAuthRestController {

	private final KakaoOAuthProperties kakao;
	private final GoogleOAuthProperties google;
	private final OAuthAccountService oauthService;
	private final JwtProvider jwtProvider;

	@GetMapping("/kakao/callback")
	public ApiResponse<Map<String, Object>> kakaoCallback(@RequestParam("code") String code) throws Exception {

		RestTemplate rest = new RestTemplate();

		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("grant_type", "authorization_code");
		params.add("client_id", kakao.getClientId());
		params.add("client_secret", kakao.getClientSecret());
		params.add("redirect_uri", kakao.getRedirectUri());
		params.add("code", code);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		HttpEntity<MultiValueMap<String, String>> tokenRequest = new HttpEntity<>(params, headers);

		Map<String, Object> tokenResponse = rest.postForObject("https://kauth.kakao.com/oauth/token", tokenRequest,
				Map.class);

		String accessToken = (String) tokenResponse.get("access_token");

		HttpHeaders profileHeader = new HttpHeaders();
		profileHeader.set("Authorization", "Bearer " + accessToken);

		HttpEntity<Void> profileRequest = new HttpEntity<>(profileHeader);

		Map<String, Object> profile = rest
				.exchange("https://kapi.kakao.com/v2/user/me", HttpMethod.GET, profileRequest, Map.class).getBody();

		String providerUserId = String.valueOf(profile.get("id"));
		OAuthAccount exists = oauthService.getOAuthByProvider("kakao", providerUserId);
		String currentUser = getCurrentUserId();

		if (currentUser != null) {
			if (exists != null && exists.getReleaseDate() == null && !exists.getUserId().equals(currentUser)) {

				return ApiResponse.success(Map.of("status", "NEED_TRANSFER", "provider", "kakao", "providerUserId",
						providerUserId, "fromUserId", exists.getUserId(), "toUserId", currentUser));
			}

			oauthService.connectOAuthAccount(currentUser, "kakao", providerUserId);

			return ApiResponse.success(Map.of("status", "CONNECT", "userId", currentUser, "provider", "kakao",
					"providerUserId", providerUserId));
		}

		if (exists == null || exists.getReleaseDate() != null) {
			return ApiResponse
					.success(Map.of("status", "NEED_REGISTER", "provider", "kakao", "providerUserId", providerUserId));
		}

		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(exists.getUserId(),
				null, List.of(() -> "ROLE_USER"));

		var tokenResponseJwt = jwtProvider.generateToken(authentication);

		return ApiResponse.success(Map.of("status", "LOGIN", "userId", exists.getUserId(), "provider", "kakao",
				"providerUserId", providerUserId, "accessToken", tokenResponseJwt.getAccessToken(), "refreshToken",
				tokenResponseJwt.getRefreshToken(), "accessTokenExpiresIn",
				tokenResponseJwt.getAccessTokenExpiresIn()));
	}

	@PostMapping("/connect")
	public ApiResponse<String> connect(@RequestBody Map<String, String> body) {
		String provider = body.get("provider");
		String providerUserId = body.get("providerUserId");
		String userId = body.get("userId");

		OAuthAccount oauth = OAuthAccount.builder().oauthId(UUID.randomUUID().toString()).provider(provider)
				.providerUserId(providerUserId).userId(userId).build();

		oauthService.addOAuthAccount(oauth);

		return ApiResponse.success("OK");
	}

	@PostMapping("/release")
	public ApiResponse<String> release(@RequestBody Map<String, String> body) {
		String oauthId = body.get("oauthId");
		oauthService.releaseOAuth(oauthId);
		return ApiResponse.success("OK");
	}

	@PostMapping("/transfer")
	public ApiResponse<Map<String, Object>> transfer(@RequestBody Map<String, String> body) {
		String currentUser = getCurrentUserId();
		if (currentUser == null) {
			return ApiResponse.error(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
		}

		String provider = body.get("provider");
		String providerUserId = body.get("providerUserId");
		String fromUserId = body.get("fromUserId");

		if (provider == null || providerUserId == null || fromUserId == null) {
			return ApiResponse.error(ErrorCode.CONFLICT, "요청 정보가 올바르지 않습니다.");
		}

		if (currentUser.equals(fromUserId)) {
			return ApiResponse.error(ErrorCode.CONFLICT, "같은 계정으로는 이전할 수 없습니다.");
		}

		oauthService.transferOAuthAccount(provider, providerUserId, fromUserId, currentUser);

		return ApiResponse.success(Map.of("status", "TRANSFER_COMPLETED", "provider", provider, "providerUserId",
				providerUserId, "fromUserId", fromUserId, "toUserId", currentUser));
	}

	@GetMapping("/google/auth")
	public void googleAuth(@RequestParam(value = "mode", required = false) String mode, HttpServletResponse response)
			throws Exception {
		String redirectUri = google.getRedirectUri();
		String clientId = google.getClientId();
		String scope = URLEncoder.encode("openid email profile", StandardCharsets.UTF_8);
		String state = mode != null ? mode : "login";

		String url = "https://accounts.google.com/o/oauth2/v2/auth" + "?client_id=" + clientId + "&redirect_uri="
				+ URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) + "&response_type=code" + "&scope=" + scope
				+ "&access_type=offline" + "&prompt=consent" + "&state=" + state;

		response.sendRedirect(url);
	}

	@GetMapping("/google/callback")
	public void googleCallback(@RequestParam("code") String code,
			@RequestParam(value = "state", required = false) String state, HttpServletResponse response)
			throws Exception {

		String currentUser = getCurrentUserId();

		RestTemplate rest = new RestTemplate();

		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("grant_type", "authorization_code");
		params.add("client_id", google.getClientId());
		params.add("client_secret", google.getClientSecret());
		params.add("redirect_uri", google.getRedirectUri());
		params.add("code", code);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		HttpEntity<MultiValueMap<String, String>> tokenRequest = new HttpEntity<>(params, headers);

		Map<String, Object> tokenResponse = rest.postForObject("https://oauth2.googleapis.com/token", tokenRequest,
				Map.class);

		String accessToken = (String) tokenResponse.get("access_token");

		HttpHeaders profileHeader = new HttpHeaders();
		profileHeader.set("Authorization", "Bearer " + accessToken);

		HttpEntity<Void> profileRequest = new HttpEntity<>(profileHeader);

		Map<String, Object> profile = rest
				.exchange("https://openidconnect.googleapis.com/v1/userinfo", HttpMethod.GET, profileRequest, Map.class)
				.getBody();

		String providerUserId = (String) profile.get("sub");

		OAuthAccount exists = oauthService.getOAuthByProvider("google", providerUserId);

		if (exists != null && "anonymousUser".equals(exists.getUserId())) {
			oauthService.releaseOAuth(exists.getOauthId());
			exists = null;
		}

		String status;
		String userId = null;
		String fromUserId = null;
		String jwtAccessToken = null;
		String jwtRefreshToken = null;
		Long jwtAccessTokenExpiresIn = null;

		if (currentUser != null) {
			if (exists != null && !exists.getUserId().equals(currentUser)) {
				status = "NEED_TRANSFER";
				userId = currentUser;
				fromUserId = exists.getUserId();
			} else {
				oauthService.connectOAuthAccount(currentUser, "google", providerUserId);
				status = "CONNECT";
				userId = currentUser;
			}
		} else {
			if (exists == null) {
				status = "NEED_REGISTER";
			} else {
				if (exists.getReleaseDate() != null) {
					oauthService.connectOAuthAccount(exists.getUserId(), "google", providerUserId);
				}
				status = "LOGIN";
				userId = exists.getUserId();

				UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userId,
						null, List.of(() -> "ROLE_USER"));

				var tokenResponseJwt = jwtProvider.generateToken(authentication);
				jwtAccessToken = tokenResponseJwt.getAccessToken();
				jwtRefreshToken = tokenResponseJwt.getRefreshToken();
				jwtAccessTokenExpiresIn = tokenResponseJwt.getAccessTokenExpiresIn();
			}
		}

		StringBuilder redirect = new StringBuilder("https://localhost:5173/oauth/google");
		redirect.append("?status=").append(status);
		redirect.append("&mode=").append(currentUser != null ? "connect" : "login");
		redirect.append("&provider=google");
		redirect.append("&providerUserId=").append(URLEncoder.encode(providerUserId, StandardCharsets.UTF_8));

		if (userId != null) {
			redirect.append("&userId=").append(URLEncoder.encode(userId, StandardCharsets.UTF_8));
		}
		if (fromUserId != null) {
			redirect.append("&fromUserId=").append(URLEncoder.encode(fromUserId, StandardCharsets.UTF_8));
		}
		if (jwtAccessToken != null) {
			redirect.append("&accessToken=").append(URLEncoder.encode(jwtAccessToken, StandardCharsets.UTF_8));
			redirect.append("&refreshToken=").append(URLEncoder.encode(jwtRefreshToken, StandardCharsets.UTF_8));
			redirect.append("&accessTokenExpiresIn=").append(jwtAccessTokenExpiresIn);
		}

		response.sendRedirect(redirect.toString());
	}

	@GetMapping("/list")
	public ApiResponse<List<OAuthAccount>> getOAuthList() {
		ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		HttpSession session = attrs.getRequest().getSession();
		String userId = (String) session.getAttribute("LOGIN_USER_ID");

		if (userId == null) {
			return ApiResponse.<List<OAuthAccount>>error(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
		}

		List<OAuthAccount> list = oauthService.getOAuthAccountList(userId);

		return ApiResponse.success(list);
	}

	private String getCurrentUserId() {
		ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		if (attrs != null) {
			var request = attrs.getRequest();
			Object attr = request.getAttribute("LOGIN_USER_ID");
			if (attr instanceof String) {
				return (String) attr;
			}
		}

		var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext()
				.getAuthentication();

		if (authentication == null || !authentication.isAuthenticated()) {
			return null;
		}

		String name = authentication.getName();
		if ("anonymousUser".equals(name)) {
			return null;
		}
		return name;
	}
}
