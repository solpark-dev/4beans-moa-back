package com.moa.common.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LogAspect {

	private static final Logger log = LoggerFactory.getLogger(LogAspect.class);

	@Around("execution(* com.moa.web..*(..)) || execution(* com.moa.service..*(..))")
	public Object logging(ProceedingJoinPoint pjp) throws Throwable {
		long start = System.currentTimeMillis();
		String name = pjp.getSignature().toShortString();
		log.info("START {}", name);
		try {
			Object result = pjp.proceed();
			long time = System.currentTimeMillis() - start;
			log.info("END {} ({}ms)", name, time);
			return result;
		} catch (Throwable t) {
			log.error("ERROR {}", name, t);
			throw t;
		}
	}
}
