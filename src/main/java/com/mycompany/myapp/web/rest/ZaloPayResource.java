package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.service.ZaloPayService;
import java.util.Map;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/zalopay")
public class ZaloPayResource {

    private static final Logger log = LoggerFactory.getLogger(ZaloPayResource.class);
    private final ZaloPayService zaloPayService;

    public ZaloPayResource(ZaloPayService zaloPayService) {
        this.zaloPayService = zaloPayService;
    }

    @PostMapping("/create-order")
    public ResponseEntity<Map<String, Object>> createOrder(@RequestParam Long orderId) {
        log.info("REST request to create ZaloPay order for Order ID: {}", orderId);
        try {
            Map<String, Object> result = zaloPayService.createOrder(orderId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error creating ZaloPay order", e);
            return ResponseEntity.badRequest().body(Map.of("return_code", -1, "return_message", e.getMessage()));
        }
    }

    @PostMapping("/callback")
    public String callback(@RequestBody String jsonStr) {
        log.info("Received ZaloPay callback");
        JSONObject result = zaloPayService.verifyCallback(jsonStr);
        return result.toString();
    }

    @GetMapping("/status/{appTransId}")
    public ResponseEntity<Map<String, Object>> checkPaymentStatus(@PathVariable String appTransId) {
        log.info("REST request to check payment status for app_trans_id: {}", appTransId);
        Map<String, Object> result = zaloPayService.checkPaymentStatus(appTransId);
        return ResponseEntity.ok(result);
    }
}
