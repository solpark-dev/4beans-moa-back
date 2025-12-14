package com.moa.auth.filter;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.moa.auth.provider.JwtProvider;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtProvider jwtProvider;

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getRequestURI();

		if (path.startsWith("/swagger-ui/") || path.startsWith("/v3/api-docs"))
			return true;

		if (path.startsWith("/api/signup/"))
			return true;

		if (path.equals("/api/auth/login"))
			return true;
		if (path.equals("/api/auth/login/otp-verify"))
			return true;
		if (path.equals("/api/auth/login/backup-verify"))
			return true;
		if (path.equals("/api/auth/refresh"))
			return true;
		if (path.equals("/api/auth/verify-email"))
			return true;
		if (path.equals("/api/auth/unlock"))
			return true;

		if (path.startsWith("/api/oauth/")) {
			boolean isStart = path.endsWith("/auth") || path.endsWith("/start") || path.contains("/auth/");
			boolean isCallback = path.contains("/callback");
			return isStart || isCallback;
		}

		return false;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		try {
			String jwt = resolveToken(request);

			if (StringUtils.hasText(jwt) && jwtProvider.validateToken(jwt)) {
				Authentication authentication = jwtProvider.getAuthentication(jwt);
				SecurityContextHolder.getContext().setAuthentication(authentication);
			}
		} catch (Exception e) {
			SecurityContextHolder.clearContext();
		}

		filterChain.doFilter(request, response);
	}

	private String resolveToken(HttpServletRequest request) {
		String bearerToken = request.getHeader("Authorization");
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
			return bearerToken.substring(7);
		}

		try {
			if (request.getCookies() != null) {
				for (var c : request.getCookies()) {
					if ("ACCESS_TOKEN".equals(c.getName()) && StringUtils.hasText(c.getValue())) {
						return c.getValue();
					}
				}
			}
		} catch (Exception e) {
		}

		return null;
	}
}
