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

		if (path.equals("/api/auth/login") || path.equals("/api/auth/login/otp-verify")
				|| path.equals("/api/auth/login/backup-verify") || path.equals("/api/auth/refresh")
				|| path.equals("/api/auth/verify-email") || path.equals("/api/auth/unlock")) {
			return true;
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
			log.warn("JWT authentication failed: {}", e.getMessage());
			SecurityContextHolder.clearContext();
		}

		filterChain.doFilter(request, response);
	}

	private String resolveToken(HttpServletRequest request) {

		String bearerToken = request.getHeader("Authorization");
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
			return bearerToken.substring(7);
		}

		if (request.getCookies() != null) {
			for (var c : request.getCookies()) {
				String name = c.getName();
				if (("ACCESS_TOKEN".equals(name) || "accessToken".equals(name)) && StringUtils.hasText(c.getValue())) {
					return c.getValue();
				}
			}
		}

		return null;
	}

}
