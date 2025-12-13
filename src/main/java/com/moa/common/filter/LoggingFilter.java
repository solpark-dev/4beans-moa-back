package com.moa.common.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 모든 HTTP 요청에 TraceId와 UserId를 MDC에 주입하는 필터.
 * 로그에서 요청 흐름을 추적할 수 있게 합니다.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LoggingFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_KEY = "traceId";
    private static final String USER_ID_KEY = "userId";
    private static final String REQUEST_URI_KEY = "requestUri";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        // TraceId 생성 (8자리 UUID)
        String traceId = generateTraceId();

        try {
            // MDC에 TraceId 설정
            MDC.put(TRACE_ID_KEY, traceId);
            MDC.put(REQUEST_URI_KEY, request.getRequestURI());

            // Response Header에 TraceId 추가 (프론트엔드에서 참조 가능)
            response.setHeader("X-Trace-Id", traceId);

            // 필터 체인 실행 전 UserId 설정 시도 (아직 인증 전일 수 있음)
            updateUserIdInMDC();

            // 요청 로깅
            logRequest(request);

            // 필터 체인 실행
            filterChain.doFilter(request, response);

            // 필터 체인 실행 후 UserId 재설정 (인증 후)
            updateUserIdInMDC();

            // 응답 로깅
            long duration = System.currentTimeMillis() - startTime;
            logResponse(request, response, duration);

        } finally {
            // MDC 정리 (메모리 누수 방지)
            MDC.clear();
        }
    }

    /**
     * 8자리 짧은 TraceId 생성
     */
    private String generateTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * SecurityContext에서 UserId를 가져와 MDC에 설정
     */
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

    /**
     * 요청 로깅 (민감 정보 제외)
     */
    private void logRequest(HttpServletRequest request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        String clientIp = getClientIp(request);

        if (shouldLog(uri)) {
            if (queryString != null) {
                // 민감 정보 마스킹
                queryString = maskSensitiveParams(queryString);
                log.info(">>> {} {}?{} [IP: {}]", method, uri, queryString, clientIp);
            } else {
                log.info(">>> {} {} [IP: {}]", method, uri, clientIp);
            }
        }
    }

    /**
     * 응답 로깅
     */
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

    /**
     * 로깅 대상 여부 확인 (정적 리소스 제외)
     */
    private boolean shouldLog(String uri) {
        return uri != null
                && !uri.contains("/uploads/")
                && !uri.contains("/static/")
                && !uri.contains("/favicon")
                && !uri.endsWith(".js")
                && !uri.endsWith(".css")
                && !uri.endsWith(".png")
                && !uri.endsWith(".jpg")
                && !uri.endsWith(".ico");
    }

    /**
     * 클라이언트 IP 추출 (프록시 환경 고려)
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For에 여러 IP가 있는 경우 첫 번째 IP 사용
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 민감 정보 마스킹 (password, token 등)
     */
    private String maskSensitiveParams(String queryString) {
        return queryString
                .replaceAll("(?i)(password|pwd|token|secret|key)=[^&]*", "$1=***")
                .replaceAll("(?i)(cardNumber|billingKey)=[^&]*", "$1=***");
    }
}
