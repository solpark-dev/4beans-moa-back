package com.moa.common.filter;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LoggingFilter extends OncePerRequestFilter {

	private static final String TRACE_ID_KEY = "traceId";
	private static final String USER_ID_KEY = "userId";
	private static final String REQUEST_URI_KEY = "requestUri";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		long startTime = System.currentTimeMillis();

		String traceId = generateTraceId();

		try {

			MDC.put(TRACE_ID_KEY, traceId);
			MDC.put(REQUEST_URI_KEY, request.getRequestURI());

			response.setHeader("X-Trace-Id", traceId);

			updateUserIdInMDC();
			logRequest(request);

			filterChain.doFilter(request, response);
			updateUserIdInMDC();
			long duration = System.currentTimeMillis() - startTime;
			logResponse(request, response, duration);

		} finally {
			MDC.clear();
		}
	}

	private String generateTraceId() {
		return UUID.randomUUID().toString().substring(0, 8);
	}

	private void updateUserIdInMDC() {
		try {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			if (authentication != null && authentication.isAuthenticated()
					&& !"anonymousUser".equals(authentication.getPrincipal())) {
				String userId = authentication.getName();
				MDC.put(USER_ID_KEY, userId);
			} else {
				MDC.put(USER_ID_KEY, "anonymous");
			}
		} catch (Exception e) {
			MDC.put(USER_ID_KEY, "unknown");
		}
	}

	private void logRequest(HttpServletRequest request) {
		String method = request.getMethod();
		String uri = request.getRequestURI();
		String queryString = request.getQueryString();
		String clientIp = getClientIp(request);

		if (shouldLog(uri)) {
			if (queryString != null) {
				queryString = maskSensitiveParams(queryString);
				log.info(">>> {} {}?{} [IP: {}]", method, uri, queryString, clientIp);
			} else {
				log.info(">>> {} {} [IP: {}]", method, uri, clientIp);
			}
		}
	}

	private void logResponse(HttpServletRequest request, HttpServletResponse response, long duration) {
		String uri = request.getRequestURI();
		int status = response.getStatus();

		if (shouldLog(uri)) {
			if (status >= 400) {
				log.warn("<<< {} {} - {}ms", status, uri, duration);
			} else {
				log.info("<<< {} {} - {}ms", status, uri, duration);
			}
		}
	}

	private boolean shouldLog(String uri) {
		return uri != null && !uri.contains("/uploads/") && !uri.contains("/static/") && !uri.contains("/favicon")
				&& !uri.endsWith(".js") && !uri.endsWith(".css") && !uri.endsWith(".png") && !uri.endsWith(".jpg")
				&& !uri.endsWith(".ico");
	}

	private String getClientIp(HttpServletRequest request) {
		String ip = request.getHeader("X-Forwarded-For");
		if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("X-Real-IP");
		}
		if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getRemoteAddr();
		}
		if (ip != null && ip.contains(",")) {
			ip = ip.split(",")[0].trim();
		}
		return ip;
	}

	private String maskSensitiveParams(String queryString) {
		return queryString.replaceAll("(?i)(password|pwd|token|secret|key)=[^&]*", "$1=***")
				.replaceAll("(?i)(cardNumber|billingKey)=[^&]*", "$1=***");
	}
}
