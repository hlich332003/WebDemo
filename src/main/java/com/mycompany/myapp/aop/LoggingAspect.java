package com.mycompany.myapp.aop;

import java.util.Arrays;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    // Áp dụng cho tất cả methods trong package service
    @Pointcut("within(com.mycompany.myapp.service..*)")
    public void servicePointcut() {}

    // Áp dụng cho tất cả methods trong package web.rest
    @Pointcut("within(com.mycompany.myapp.web.rest..*)")
    public void restPointcut() {}

    // Log khi có exception
    @AfterThrowing(pointcut = "servicePointcut() || restPointcut()", throwing = "e")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable e) {
        log.error(
            "❌ Exception in {}.{}() with message: {}",
            joinPoint.getSignature().getDeclaringTypeName(),
            joinPoint.getSignature().getName(),
            e.getMessage()
        );
    }

    // Log trước và sau khi method chạy
    @Around("servicePointcut() || restPointcut()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();

        // Log trước khi chạy
        log.debug("🔵 Enter: {}.{}() with arguments: {}", className, methodName, Arrays.toString(joinPoint.getArgs()));

        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed(); // Chạy method
            long duration = System.currentTimeMillis() - startTime;

            // Log sau khi chạy thành công
            log.debug("✅ Exit: {}.{}() - Duration: {}ms", className, methodName, duration);

            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;

            // Log khi có lỗi
            log.error("❌ Exception: {}.{}() - Duration: {}ms - Error: {}", className, methodName, duration, e.getMessage());

            throw e;
        }
    }
}
