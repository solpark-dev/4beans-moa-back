package com.moa.web.oauth;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
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
import com.moa.common.exception.BusinessException;
import com.moa.common.exception.ErrorCode;
import com.moa.config.GoogleOAuthProperties;
import com.moa.config.KakaoOAuthProperties;
import com.moa.domain.OAuthAccount;
import com.moa.service.auth.LoginHistoryService;
import com.moa.service.oauth.OAuthAccountService;
import com.moa.service.user.UserService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/oauth")
@RequiredArgsConstructor
public class OAuthRestController {

	@Value("${app.frontend-url}")
	private String frontendUrl;

	private final KakaoOAuthProperties kakao;
	private final GoogleOAuthProperties google;
	private final OAuthAccountService oauthService;
	private final LoginHistoryService loginHistoryService;
	private final JwtProvider jwtProvider;
	private final UserService userService;

	@GetMapping("/kakao/auth")
	public ApiResponse<?> kakaoAuth(@RequestParam(defaultValue = "login") String mode) {

		String redirectUri = kakao.getRedirectUri();

		String url = "https://kauth.kakao.com/oauth/authorize" + "?client_id=" + kakao.getClientId() + "&redirect_uri="
				+ URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) + "&response_type=code" + "&state=" + mode;

		return ApiResponse.success(Map.of("url", url));
	}

	@GetMapping("/kakao/callback")
	public ResponseEntity<Void> kakaoCallback(
	        @RequestParam("code") String code,
	        @RequestParam(defaultValue = "login") String mode) {

	    RestTemplate rest = new RestTemplate();

	    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
	    params.add("grant_type", "authorization_code");
	    params.add("client_id", kakao.getClientId());
	    params.add("client_secret", kakao.getClientSecret());
	    params.add("redirect_uri", kakao.getRedirectUri());
	    params.add("code", code);

	    HttpHeaders headers = new HttpHeaders();
	    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

	    Map<String, Object> tokenResponse = rest.postForObject(
	            "https://kauth.kakao.com/oauth/token",
	            new HttpEntity<>(params, headers),
	            Map.class
	    );

	    String kakaoAccessToken = (String) tokenResponse.get("access_token");

	    HttpHeaders profileHeader = new HttpHeaders();
	    profileHeader.setBearerAuth(kakaoAccessToken);

	    Map<String, Object> profile = rest.exchange(
	            "https://kapi.kakao.com/v2/user/me",
	            HttpMethod.GET,
	            new HttpEntity<>(profileHeader),
	            Map.class
	    ).getBody();

	    String provider = "kakao";
	    String providerUserId = String.valueOf(profile.get("id"));

	    Map<String, Object> kakaoAccount =
	            (Map<String, Object>) profile.get("kakao_account");

	    String email = kakaoAccount != null ? (String) kakaoAccount.get("email") : null;

	    if (email == null || email.isBlank()) {
	        throw new BusinessException(
	                ErrorCode.BAD_REQUEST,
	                "카카오 이메일 제공에 동의해야 가입할 수 있습니다."
	        );
	    }

	    OAuthAccount oauth = oauthService.getOAuthByProvider(provider, providerUserId);
	    String currentUserId = getCurrentUserId();
	    String redirectBase = frontendUrl + "/oauth/callback";

	    if (currentUserId != null) {
	        oauthService.connectOAuthAccount(currentUserId, provider, providerUserId);
	        return redirect(redirectBase + "?status=CONNECT&provider=kakao");
	    }

	    if (oauth != null && oauth.getReleaseDate() == null) {

	        UsernamePasswordAuthenticationToken auth =
	                new UsernamePasswordAuthenticationToken(
	                        oauth.getUserId(),
	                        null,
	                        List.of(() -> "ROLE_USER")
	                );

	        var token = jwtProvider.generateToken(auth);

	        loginHistoryService.recordSuccess(oauth.getUserId(), "KAKAO", null, null);

	        ResponseCookie accessCookie = ResponseCookie.from("accessToken", token.getAccessToken())
	                .httpOnly(true)
	                .secure(true)
	                .sameSite("None")
	                .path("/")
	                .maxAge(token.getAccessTokenExpiresIn())
	                .build();

	        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", token.getRefreshToken())
	                .httpOnly(true)
	                .secure(true)
	                .sameSite("None")
	                .path("/")
	                .maxAge(60 * 60 * 24 * 14)
	                .build();

	        return ResponseEntity.status(HttpStatus.FOUND)
	                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
	                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
	                .header(HttpHeaders.LOCATION, redirectBase + "?status=LOGIN")
	                .build();
	    }

	    // 3️⃣ 신규 회원 → 회원가입 유도
	    return redirect(
	            redirectBase
	                    + "?status=NEED_REGISTER"
	                    + "&provider=kakao"
	                    + "&providerUserId=" + providerUserId
	                    + "&email=" + URLEncoder.encode(email, StandardCharsets.UTF_8)
	    );
	}


	private ResponseEntity<Void> redirect(String url) {
		return ResponseEntity.status(302).header(HttpHeaders.LOCATION, url).build();
	}

	@GetMapping("/google/auth")
	public ApiResponse<?> googleAuth(@RequestParam(defaultValue = "login") String mode) {

		String scope = URLEncoder.encode("openid email profile", StandardCharsets.UTF_8);
		String redirectUri = google.getRedirectUri();

		String url = "https://accounts.google.com/o/oauth2/v2/auth" + "?client_id=" + google.getClientId()
				+ "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) + "&response_type=code"
				+ "&scope=" + scope + "&access_type=offline" + "&prompt=consent" + "&state=" + mode;

		return ApiResponse.success(Map.of("url", url));
	}

	@GetMapping("/google/callback")
	public ResponseEntity<Void> googleCallback(@RequestParam("code") String code,
			@RequestParam(defaultValue = "login") String mode) throws Exception {

		RestTemplate rest = new RestTemplate();

		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("grant_type", "authorization_code");
		params.add("client_id", google.getClientId());
		params.add("client_secret", google.getClientSecret());
		params.add("code", code);
		params.add("redirect_uri", google.getRedirectUri());

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		HttpEntity<MultiValueMap<String, String>> tokenRequest = new HttpEntity<>(params, headers);

		Map<String, Object> tokenResponse = rest.postForObject("https://oauth2.googleapis.com/token", tokenRequest,
				Map.class);

		String accessToken = (String) tokenResponse.get("access_token");

		HttpHeaders profileHeader = new HttpHeaders();
		profileHeader.setBearerAuth(accessToken);

		HttpEntity<Void> profileRequest = new HttpEntity<>(profileHeader);

		Map<String, Object> profile = rest
				.exchange("https://openidconnect.googleapis.com/v1/userinfo", HttpMethod.GET, profileRequest, Map.class)
				.getBody();

		String providerUserId = (String) profile.get("sub");

		OAuthAccount exists = oauthService.getOAuthByProvider("google", providerUserId);
		String currentUser = getCurrentUserId();

		String redirectBase = frontendUrl + "/oauth/callback";

		if (currentUser != null) {

			if (exists != null && exists.getReleaseDate() == null && !exists.getUserId().equals(currentUser)) {

				String redirectUrl = redirectBase + "?status=NEED_TRANSFER" + "&provider=google" + "&providerUserId="
						+ providerUserId;

				return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, redirectUrl).build();
			}

			oauthService.connectOAuthAccount(currentUser, "google", providerUserId);
			loginHistoryService.recordSuccess(currentUser, "GOOGLE", null, null);

			return ResponseEntity.status(HttpStatus.FOUND)
					.header(HttpHeaders.LOCATION, redirectBase + "?status=CONNECT").build();
		}

		if (exists == null || exists.getReleaseDate() != null) {
			String email = (String) profile.get("email");
			String redirectUrl = 
					redirectBase 
					+ "?status=NEED_REGISTER" 
					+ "&provider=google" 
					+ "&providerUserId="
					+ providerUserId
					+ "&email=" + URLEncoder.encode(email, StandardCharsets.UTF_8);

			return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, redirectUrl).build();
		}

		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(exists.getUserId(),
				null, List.of(() -> "ROLE_USER"));

		var jwt = jwtProvider.generateToken(authentication);

		String redirectUrl = redirectBase + "?status=LOGIN" + "&accessToken=" + jwt.getAccessToken() + "&refreshToken="
				+ jwt.getRefreshToken() + "&expiresIn=" + jwt.getAccessTokenExpiresIn();

		return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, redirectUrl).build();
	}

	@PostMapping("/connect")
	public ApiResponse<String> connect(@RequestBody Map<String, String> body) {
		String provider = body.get("provider");
		String providerUserId = body.get("providerUserId");

		String currentUser = getCurrentUserId();
		if (currentUser == null) {
			return ApiResponse.error(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
		}

		OAuthAccount existing = oauthService.getOAuthByProvider(provider, providerUserId);
		if (existing != null && existing.getReleaseDate() == null && !existing.getUserId().equals(currentUser)) {
			return ApiResponse.error(ErrorCode.CONFLICT, "이미 다른 계정에 연결된 소셜 계정입니다.");
		}

		oauthService.connectOAuthAccount(currentUser, provider, providerUserId);
		return ApiResponse.success("OK");
	}

	@PostMapping("/connect-by-phone")
	public ApiResponse<Map<String, Object>> connectByPhone(@RequestBody Map<String, String> body) {
		String provider = body.get("provider");
		String providerUserId = body.get("providerUserId");
		String phone = body.get("phone");

		if (provider == null || providerUserId == null || phone == null) {
			return ApiResponse.error(ErrorCode.INVALID_PARAMETER, "요청 정보가 올바르지 않습니다.");
		}

		var userOpt = userService.findByPhone(phone);
		if (userOpt.isEmpty()) {
			return ApiResponse.error(ErrorCode.NOT_FOUND, "해당 휴대폰 번호로 가입된 계정이 없습니다.");
		}

		var user = userOpt.get();

		OAuthAccount existing = oauthService.getOAuthByProvider(provider, providerUserId);
		if (existing != null && existing.getReleaseDate() == null && !existing.getUserId().equals(user.getUserId())) {
			return ApiResponse.error(ErrorCode.CONFLICT, "이미 다른 계정에 연결된 소셜 계정입니다.");
		}

		oauthService.connectOAuthAccount(user.getUserId(), provider, providerUserId);

		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(user.getUserId(),
				null, List.of(() -> "ROLE_USER"));

		var tokenResponse = jwtProvider.generateToken(authentication);

		return ApiResponse.success(Map.of("status", "LOGIN", "userId", user.getUserId(), "provider", provider,
				"providerUserId", providerUserId, "accessToken", tokenResponse.getAccessToken(), "refreshToken",
				tokenResponse.getRefreshToken(), "accessTokenExpiresIn", tokenResponse.getAccessTokenExpiresIn()));
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

	@GetMapping("/list")
	public ApiResponse<List<OAuthAccount>> getOAuthList() {
		ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

		HttpSession session = attrs.getRequest().getSession();
		String userId = (String) session.getAttribute("LOGIN_USER_ID");

		if (userId == null) {
			return ApiResponse.error(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
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