package com.moa.common.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Aspect
@Component
public class LogAspect {

	private static final long SLOW_EXECUTION_THRESHOLD_MS = 3000; // 3초 이상이면 경고

	/**
	 * 서비스 레이어 메서드 실행 로깅
	 */
	@Around("execution(* com.moa.service..*(..))")
	public Object logServiceExecution(ProceedingJoinPoint pjp) throws Throwable {
		MethodSignature signature = (MethodSignature) pjp.getSignature();
		String className = signature.getDeclaringType().getSimpleName();
		String methodName = signature.getName();
		String fullName = className + "." + methodName;

		// DEBUG 레벨: 메서드 시작 (파라미터 포함, 민감 정보 제외)
		if (log.isDebugEnabled()) {
			String args = maskSensitiveArgs(pjp.getArgs());
			log.debug("▶ {} ARGS: {}", fullName, args);
		}

		long startTime = System.currentTimeMillis();

		try {
			Object result = pjp.proceed();
			long executionTime = System.currentTimeMillis() - startTime;

			// 느린 실행 경고
			if (executionTime > SLOW_EXECUTION_THRESHOLD_MS) {
				log.warn("◀ {} SLOW: {}ms", fullName, executionTime);
			} else if (log.isDebugEnabled()) {
				log.debug("◀ {} OK: {}ms", fullName, executionTime);
			}

			return result;

		} catch (Throwable t) {
			long executionTime = System.currentTimeMillis() - startTime;
			log.error("✖ {} FAILED: {}ms - {}: {}",
					fullName, executionTime, t.getClass().getSimpleName(), t.getMessage());
			throw t;
		}
	}

	/**
	 * 결제/정산 관련 메서드는 INFO 레벨로 항상 로깅
	 */
	@Around("execution(* com.moa.service.payment..*(..)) || " +
			"execution(* com.moa.service.settlement..*(..)) || " +
			"execution(* com.moa.service.deposit..*(..))")
	public Object logPaymentExecution(ProceedingJoinPoint pjp) throws Throwable {
		MethodSignature signature = (MethodSignature) pjp.getSignature();
		String className = signature.getDeclaringType().getSimpleName();
		String methodName = signature.getName();
		String fullName = className + "." + methodName;

		log.info("💰 [PAYMENT] ▶ {}", fullName);

		long startTime = System.currentTimeMillis();

		try {
			Object result = pjp.proceed();
			long executionTime = System.currentTimeMillis() - startTime;
			log.info("💰 [PAYMENT] ◀ {} OK: {}ms", fullName, executionTime);
			return result;

		} catch (Throwable t) {
			long executionTime = System.currentTimeMillis() - startTime;
			log.error("💰 [PAYMENT] ✖ {} FAILED: {}ms - {}: {}",
					fullName, executionTime, t.getClass().getSimpleName(), t.getMessage());
			throw t;
		}
	}

	/**
	 * 민감 정보 마스킹 (password, token, billingKey 등)
	 */
	private String maskSensitiveArgs(Object[] args) {
		if (args == null || args.length == 0) {
			return "[]";
		}

		return Arrays.stream(args)
				.map(arg -> {
					if (arg == null)
						return "null";
					String str = arg.toString();
					// 일반적인 민감 정보 마스킹
					str = str.replaceAll("(?i)(password|pwd)=[^,}\\]]*", "$1=***");
					str = str.replaceAll("(?i)(token|secret|key|billingKey)=[^,}\\]]*", "$1=***");
					// 너무 긴 문자열 축약
					if (str.length() > 200) {
						str = str.substring(0, 200) + "...(truncated)";
					}
					return str;
				})
				.toList()
				.toString();
	}
}
