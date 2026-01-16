package com.mycompany.myapp.web.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.service.ZaloPayService;
import com.mycompany.myapp.service.dto.ZaloPayOrderRequest;
import com.mycompany.myapp.service.dto.ZaloPayOrderResponse;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for ZaloPay payment integration
 */
@RestController
@RequestMapping("/api/zalopay")
public class ZaloPayResource {

    private static final Logger log = LoggerFactory.getLogger(ZaloPayResource.class);

    private final ZaloPayService zaloPayService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ZaloPayResource(ZaloPayService zaloPayService) {
        this.zaloPayService = zaloPayService;
    }

    /**
     * POST /api/zalopay/create-payment : Create a new payment order
     *
     * @param request the payment order request
     * @return the ResponseEntity with status 200 (OK) and order URL in body, or status 400 (Bad Request) if error
     */
    @PostMapping("/create-payment")
    public ResponseEntity<ZaloPayOrderResponse> createPayment(@RequestBody ZaloPayOrderRequest request) {
        log.debug("REST request to create ZaloPay payment : {}", request);

        try {
            // Validate request
            if (request.getAmount() == null || request.getAmount() <= 0) {
                ZaloPayOrderResponse errorResponse = new ZaloPayOrderResponse();
                errorResponse.setReturnCode(-1);
                errorResponse.setReturnMessage("Amount must be greater than 0");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            if (request.getAppUser() == null || request.getAppUser().isEmpty()) {
                ZaloPayOrderResponse errorResponse = new ZaloPayOrderResponse();
                errorResponse.setReturnCode(-1);
                errorResponse.setReturnMessage("App user is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            if (request.getOrderCode() == null || request.getOrderCode().isEmpty()) {
                // Generate random order code if not provided
                request.setOrderCode(String.valueOf(System.currentTimeMillis()));
            }

            if (request.getDescription() == null || request.getDescription().isEmpty()) {
                request.setDescription("Payment for order " + request.getOrderCode());
            }

            // Create order
            ZaloPayOrderResponse response = zaloPayService.createOrder(request);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error creating ZaloPay payment", e);
            ZaloPayOrderResponse errorResponse = new ZaloPayOrderResponse();
            errorResponse.setReturnCode(-1);
            errorResponse.setReturnMessage("Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * POST /api/zalopay/callback : Receive callback from ZaloPay after payment
     *
     * @param callbackData the callback data from ZaloPay
     * @return the ResponseEntity with status 200 (OK)
     */
    @PostMapping("/callback")
    public ResponseEntity<Map<String, Object>> callback(@RequestBody Map<String, Object> callbackData) {
        log.info("Received ZaloPay callback: {}", callbackData);

        Map<String, Object> result = new HashMap<>();

        try {
            String dataStr = (String) callbackData.get("data");
            String reqMac = (String) callbackData.get("mac");

            // Verify MAC
            boolean isValid = zaloPayService.verifyCallback(dataStr, reqMac);

            if (isValid) {
                // Parse data
                Map<String, Object> dataJson = objectMapper.readValue(dataStr, Map.class);
                log.info("Payment successful: {}", dataJson);

                // TODO: Update order status in database
                // String appTransId = (String) dataJson.get("app_trans_id");
                // Long amount = ((Number) dataJson.get("amount")).longValue();
                // Update your order status here

                result.put("return_code", 1);
                result.put("return_message", "success");
            } else {
                log.warn("Invalid MAC from ZaloPay callback");
                result.put("return_code", -1);
                result.put("return_message", "mac not equal");
            }
        } catch (Exception e) {
            log.error("Error processing ZaloPay callback", e);
            result.put("return_code", 0);
            result.put("return_message", e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/zalopay/status/{orderCode} : Check payment status
     *
     * @param orderCode the order code
     * @return the ResponseEntity with payment status
     */
    @GetMapping("/status/{orderCode}")
    public ResponseEntity<Map<String, Object>> checkStatus(@PathVariable String orderCode) {
        log.debug("REST request to check payment status for order: {}", orderCode);

        Map<String, Object> result = new HashMap<>();
        // TODO: Implement status check logic
        // You can query from database or call ZaloPay query API

        result.put("orderCode", orderCode);
        result.put("status", "pending");
        result.put("message", "Status check not implemented yet");

        return ResponseEntity.ok(result);
    }
}
