package com.moa.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
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
						.requestMatchers(
								"/api/auth/**",
								"/api/oauth/kakao/callback",
								"/api/oauth/google/callback",
								"/api/oauth/kakao/auth",
								"/api/oauth/google/auth",
								"/api/chatbot/**",
								"/api/users/join",
								"/api/users/add",
								"/api/users/check",
								"/api/users/find-id",
								"/api/users/pass/**",
								"/api/users/resetPwd/**",
								"/api/auth/verify-email",
								"/swagger-ui/**",
								"/v3/api-docs/**",
								"/uploads/**",
								"/api/community/**" // 커뮤니티 추가
						).permitAll()

						// ========== 커뮤니티 권한 설정 세분화==========
						.requestMatchers(org.springframework.http.HttpMethod.GET, "/api/community/notice/**")
						.permitAll()
						.requestMatchers(org.springframework.http.HttpMethod.GET, "/api/community/faq/**").permitAll()
						.requestMatchers(org.springframework.http.HttpMethod.POST, "/api/community/notice/**")
						.hasAuthority("ADMIN")
						.requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/community/notice/**")
						.hasAuthority("ADMIN")
						.requestMatchers(org.springframework.http.HttpMethod.POST, "/api/community/faq/**")
						.hasAuthority("ADMIN")
						.requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/community/faq/**")
						.hasAuthority("ADMIN")
						.requestMatchers("/api/community/inquiry/**").authenticated() // 문의 로그인한 사용자만 접근 가능
						// ========== 커뮤니티 권한 설정 끝 ==========

						// 파티 목록 및 상세 조회 권한 설정 (비로그인 허용)
						.requestMatchers(org.springframework.http.HttpMethod.GET, "/api/parties").permitAll()
						.requestMatchers(org.springframework.http.HttpMethod.GET, "/api/parties/**").permitAll()

						.requestMatchers("/api/admin/**").hasAuthority("ADMIN")
						.requestMatchers(org.springframework.http.HttpMethod.GET, "/api/product/**").permitAll()
						.requestMatchers("/api/oauth/**").authenticated()
						.requestMatchers("/api/users/me").authenticated()
						.anyRequest().authenticated())
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {

		List<String> origins = Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList();

		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOriginPatterns(origins);
		config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
		config.setAllowedHeaders(Arrays.asList("*", "Authorization", "Refresh-Token"));
		config.setAllowedOriginPatterns(List.of(
				"https://localhost:5173",
				"http://localhost:5173",
				"http://192.168.*:5173",
				"https://192.168.*:5173"));
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