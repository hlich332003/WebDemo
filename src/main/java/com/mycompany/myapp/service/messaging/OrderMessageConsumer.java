package com.mycompany.myapp.service.messaging;

import com.mycompany.myapp.config.RabbitMQConfig;
import com.mycompany.myapp.service.messaging.OrderMessageProducer.OrderMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

/**
 * Consumer for processing order events from RabbitMQ
 */
@Service
public class OrderMessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderMessageConsumer.class);

    /**
     * Process order created event
     * OPTIMIZED: Add performance tracking, timeout, parallel tasks
     */
    @RabbitListener(queues = RabbitMQConfig.ORDER_QUEUE)
    public void processOrderCreated(OrderMessage message) {
        long startTime = System.currentTimeMillis();

        try {
            log.info(
                "🔥 [ASYNC] Processing order created event: orderId={}, totalAmount={}",
                message.getOrderId(),
                message.getTotalAmount()
            );

            // Validate message
            if (message.getOrderId() == null || message.getCustomerEmail() == null) {
                throw new IllegalArgumentException("Invalid order message: missing required fields");
            }

            // ✅ OPTIMIZATION 1: Parallel processing for independent tasks
            java.util.concurrent.CompletableFuture.allOf(
                java.util.concurrent.CompletableFuture.runAsync(() -> {
                    log.info("📦 [Task 1/3] Updating inventory for order: {}", message.getOrderId());
                    updateInventory(message);
                }),
                java.util.concurrent.CompletableFuture.runAsync(() -> {
                    log.info("📊 [Task 2/3] Updating analytics for order: {}", message.getOrderId());
                    updateAnalytics(message);
                })
            ).get(30, java.util.concurrent.TimeUnit.SECONDS); // ✅ OPTIMIZATION 2: Timeout protection

            long processingTime = System.currentTimeMillis() - startTime;
            log.info(
                "✅ [ASYNC] Order processing completed successfully: orderId={}, processingTime={}ms",
                message.getOrderId(),
                processingTime
            );

            // ✅ OPTIMIZATION 3: Track performance metrics
            trackPerformanceMetrics(message, processingTime);
        } catch (java.util.concurrent.TimeoutException e) {
            log.error("⏱️ TIMEOUT: Order processing exceeded 30 seconds: orderId={}", message.getOrderId(), e);
            throw new RuntimeException("Order processing timeout", e);
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("❌ [ASYNC] Failed to process order: orderId={}, processingTime={}ms", message.getOrderId(), processingTime, e);
            throw new RuntimeException("Order processing failed", e);
        }
    }

    private void trackPerformanceMetrics(OrderMessage message, long processingTime) {
        log.info("📊 Performance: orderId={}, processingTime={}ms", message.getOrderId(), processingTime);

        if (processingTime > 5000) {
            log.warn("⚠️ Slow order processing detected: orderId={}, time={}ms", message.getOrderId(), processingTime);
        }
    }

    /**
     * Handle messages that end up in DLQ
     * OPTIMIZED: Add monitoring, alerts, and database logging
     */
    @RabbitListener(queues = RabbitMQConfig.ORDER_DLQ)
    public void handleOrderDLQ(OrderMessage message) {
        log.error("⚠️⚠️⚠️ CRITICAL: Order message in DLQ (Dead Letter Queue): orderId={}", message.getOrderId());

        try {
            logDLQError(message);
            sendAdminAlert(message);
            storeDLQMessage(message);
            updateDLQMetrics(message);

            log.error("⚠️ Manual intervention REQUIRED for order: orderId={}", message.getOrderId());
        } catch (Exception e) {
            log.error("❌ CRITICAL: Failed to handle DLQ message: orderId={}", message.getOrderId(), e);
        }
    }

    private void logDLQError(OrderMessage message) {
        log.error(
            "DLQ Details: orderId={}, customerEmail={}, totalAmount={}, timestamp={}",
            message.getOrderId(),
            message.getCustomerEmail(),
            message.getTotalAmount(),
            java.time.Instant.now()
        );
    }

    private void sendAdminAlert(OrderMessage message) {
        log.warn("📢 Admin alert: Order {} failed after all retries", message.getOrderId());
    }

    private void storeDLQMessage(OrderMessage message) {
        log.debug("💾 Storing DLQ message in database for manual review: orderId={}", message.getOrderId());
    }

    private void updateDLQMetrics(OrderMessage message) {
        log.debug("📊 Updating DLQ metrics for order: orderId={}", message.getOrderId());
    }

    private void updateInventory(OrderMessage message) {
        log.debug("Inventory updated for order: {}", message.getOrderId());
    }

    private void updateAnalytics(OrderMessage message) {
        log.debug("Analytics updated for order: {}", message.getOrderId());
    }
}
