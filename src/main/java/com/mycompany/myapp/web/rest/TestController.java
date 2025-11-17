package com.mycompany.myapp.web.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test")
public class TestController {

    private final Logger log = LoggerFactory.getLogger(TestController.class);

    // private final NotificationService notificationService; // Removed

    public TestController(/*NotificationService notificationService*/) { // Removed parameter
        // this.notificationService = notificationService; // Removed
    }
    // Removed WebSocket test endpoints
    // @GetMapping("/websocket")
    // public String testWebSocket() {
    //     log.info("🔔 TEST: Sending WebSocket notification...");
    //     notificationService.notifyNewOrder(999L, "Test Customer");
    //     log.info("✅ TEST: Notification sent successfully");
    //     return "WebSocket notification sent! Check admin dashboard console.";
    // }

    // @GetMapping("/websocket/{orderId}/{customerName}")
    // public String testWebSocketWithParams(
    //     @PathVariable Long orderId,
    //     @PathVariable String customerName
    // ) {
    //     log.info("🔔 TEST: Sending WebSocket notification for order {} from {}", orderId, customerName);
    //     notificationService.notifyNewOrder(orderId, customerName);
    //     log.info("✅ TEST: Notification sent successfully");
    //     return String.format("WebSocket notification sent for order #%d from %s", orderId, customerName);
    // }
}
