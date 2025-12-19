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
				.requestMatchers(
								"/api/auth/login",
								"/api/auth/login/otp-verify",
								"/api/auth/login/backup-verify",
								"/api/auth/refresh",
								"/api/auth/verify-email",
								"/api/auth/unlock",
								 "/api/auth/restore",
								 "/api/auth/exists-by-email",
								"/api/community/**")
				.permitAll()
				.requestMatchers(
								"/api/oauth/kakao/callback",
								"/api/oauth/google/callback",
								"/api/oauth/kakao/auth",
								"/api/oauth/google/auth",
								"/oauth/google",
							    "/oauth/kakao")
				.permitAll()
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
						.requestMatchers(HttpMethod.GET, "/api/community/notice/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/community/faq/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/push/subscribe").authenticated()
						.requestMatchers(HttpMethod.POST, "/api/signup/pass/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/signup/pass/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/users/check-nickname").permitAll()
						
						.requestMatchers(HttpMethod.POST, "/api/community/notice/**").hasAuthority("ADMIN")
						.requestMatchers(HttpMethod.PUT, "/api/community/notice/**").hasAuthority("ADMIN")
						.requestMatchers(HttpMethod.POST, "/api/community/faq/**").hasAuthority("ADMIN")
						.requestMatchers(HttpMethod.PUT, "/api/community/faq/**").hasAuthority("ADMIN")

						.requestMatchers("/api/community/inquiry/**").authenticated()
						.requestMatchers("/api/admin/**").hasAuthority("ADMIN")
						.requestMatchers("/api/push/admin/**").hasAuthority("ADMIN")
						
						.requestMatchers(HttpMethod.GET, "/api/product/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/parties").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/parties/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/subscriptions/products").permitAll()

						.requestMatchers("/api/oauth/**").authenticated()

						.requestMatchers("/api/users/me").authenticated()

						.requestMatchers(
										"/api/auth/otp/setup",
										"/api/auth/otp/verify",
										"/api/auth/otp/disable",
										"/api/auth/otp/disable-verify",
										"/api/auth/otp/backup/**")
						.authenticated()

						.requestMatchers("/api/auth/logout").authenticated()

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
	    config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Refresh-Token", "Last-Event-ID", "*"));
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