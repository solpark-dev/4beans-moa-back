package com.moa.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.moa.auth.filter.JwtAuthenticationFilter;
import com.moa.auth.handler.JwtAccessDeniedHandler;
import com.moa.auth.handler.JwtAuthenticationEntryPoint;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
	private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	@Value("${app.cors.allowed-origins}")
	private String allowedOrigins;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

		http
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))
				.csrf(csrf -> csrf.disable())
				.formLogin(login -> login.disable())
				.httpBasic(basic -> basic.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.securityContext(security -> security.requireExplicitSave(false))
				.rememberMe(remember -> remember.disable())
				.exceptionHandling(exception -> exception
						.authenticationEntryPoint(jwtAuthenticationEntryPoint)
						.accessDeniedHandler(jwtAccessDeniedHandler))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/api/signup/**").permitAll()
						// 인증 불필요: 로그인/토큰/이메일 인증/잠금 해제
						.requestMatchers(
								"/api/auth/login",
								"/api/auth/login/otp-verify",
								"/api/auth/login/backup-verify",
								"/api/auth/refresh",
								"/api/auth/verify-email",
								"/api/auth/unlock",
								"/api/community/**")
						.permitAll()

				// OAuth 콜백 및 인증 시작
				.requestMatchers(
					"/api/oauth/kakao/callback",
					"/api/oauth/google/callback",
					"/api/oauth/kakao/auth",
					"/api/oauth/google/auth",
					"/oauth/google",
				    "/oauth/kakao"
				).permitAll()

						// 챗봇, 회원가입/본인인증/비밀번호 재설정
						.requestMatchers(
								"/api/chatbot/**",
								"/api/users/join",
								"/api/users/check",
								"/api/users/find-id",
								"/api/users/pass/**",
								"/api/users/resetPwd/**",
								"/api/users/exists-by-phone",
								"/api/oauth/connect-by-phone",
								"/swagger-ui/**",
								"/v3/api-docs/**",
								"/uploads/**")
						.permitAll()

						// 커뮤니티: 공지/FAQ 조회는 모두 허용
						.requestMatchers(HttpMethod.GET, "/api/community/notice/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/community/faq/**").permitAll()

						// 커뮤니티: 공지/FAQ 등록/수정은 ADMIN만
						.requestMatchers(HttpMethod.POST, "/api/community/notice/**").hasAuthority("ADMIN")
						.requestMatchers(HttpMethod.PUT, "/api/community/notice/**").hasAuthority("ADMIN")
						.requestMatchers(HttpMethod.POST, "/api/community/faq/**").hasAuthority("ADMIN")
						.requestMatchers(HttpMethod.PUT, "/api/community/faq/**").hasAuthority("ADMIN")
						
						.requestMatchers(HttpMethod.POST, "/api/signup/pass/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/signup/pass/**").permitAll()

						// 커뮤니티 문의는 로그인 사용자만
						.requestMatchers("/api/community/inquiry/**").authenticated()

						// 관리자 API
						.requestMatchers("/api/admin/**").hasAuthority("ADMIN")

						// 푸시 관리자 API
						.requestMatchers("/api/push/admin/**").hasAuthority("ADMIN")

						// 상품 조회는 모두 허용
						.requestMatchers(HttpMethod.GET, "/api/product/**").permitAll()

						// 파티 목록 조회는 모두 허용
						.requestMatchers(HttpMethod.GET, "/api/parties").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/parties/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/subscriptions/products").permitAll()

						// OAuth 관련 기타 API는 인증 필요
						.requestMatchers("/api/oauth/**").authenticated()

						// 내 정보 조회
						.requestMatchers("/api/users/me").authenticated()

						// OTP 설정/해제/백업코드 발급은 로그인 사용자만
						.requestMatchers(
								"/api/auth/otp/setup",
								"/api/auth/otp/verify",
								"/api/auth/otp/disable",
								"/api/auth/otp/disable-verify",
								"/api/auth/otp/backup/**")
						.authenticated()

						// 로그아웃 인증 필요
						.requestMatchers("/api/auth/logout").authenticated()

						// 나머지는 기본적으로 인증 필요
						.anyRequest().authenticated())
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {

	    CorsConfiguration config = new CorsConfiguration();

	    List<String> origins = List.of(allowedOrigins.split(","));
	    config.setAllowedOriginPatterns(origins);

	    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
	    config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Refresh-Token", "*"));
	    config.setAllowCredentials(true);

	    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
	    source.registerCorsConfiguration("/**", config);
	    return source;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}
}