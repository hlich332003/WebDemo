package com.mycompany.myapp.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Aspect để log execution time của các method được đánh dấu @Loggable
 * Đây là Custom Aspect theo yêu cầu lộ trình đào tạo
 *
 * Note: Không dùng @Component vì JHipster đã có LoggingAspect riêng trong aop/logging/
 */
@Aspect
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    /**
     * Around advice cho các method có annotation @Loggable
     * Log execution time và các exception nếu có
     */
    @Around("@annotation(com.mycompany.myapp.aop.Loggable)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().toShortString();

        log.debug("→ Executing: {}", methodName);

        Object result;
        try {
            result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - start;
            log.debug("← Completed: {} in {} ms", methodName, executionTime);

            // Warning nếu method chạy quá lâu (> 1000ms)
            if (executionTime > 1000) {
                log.warn("⚠️ Slow method detected: {} took {} ms", methodName, executionTime);
            }
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - start;
            log.error("❌ Failed: {} after {} ms - Error: {}", methodName, executionTime, e.getMessage());
            throw e;
        }

        return result;
    }

    /**
     * Around advice cho tất cả các method trong Service layer
     * Log performance của toàn bộ service methods
     */
    @Around("execution(* com.mycompany.myapp.service.*.*(..))")
    public Object logServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - start;

            if (executionTime > 500) {
                log.info("📊 [{}] {}.{}() executed in {} ms", executionTime > 1000 ? "SLOW" : "OK", className, methodName, executionTime);
            }

            return result;
        } catch (Exception e) {
            log.error("💥 [ERROR] {}.{}() failed: {}", className, methodName, e.getMessage());
            throw e;
        }
    }
}
