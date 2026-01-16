package com.mycompany.myapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.service.dto.ZaloPayOrderRequest;
import com.mycompany.myapp.service.dto.ZaloPayOrderResponse;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Service for handling ZaloPay payment integration
 */
@Service
public class ZaloPayService {

    private static final Logger log = LoggerFactory.getLogger(ZaloPayService.class);

    @Value("${zalopay.appid}")
    private String appId;

    @Value("${zalopay.key1}")
    private String key1;

    @Value("${zalopay.key2}")
    private String key2;

    @Value("${zalopay.endpoint}")
    private String endpoint;

    @Value("${zalopay.callback-url}")
    private String callbackUrl;

    @Value("${zalopay.redirect-url}")
    private String redirectUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Create ZaloPay payment order
     */
    public ZaloPayOrderResponse createOrder(ZaloPayOrderRequest request) throws Exception {
        log.debug("Creating ZaloPay order for: {}", request);

        // Generate app_trans_id with format: yyMMdd_xxxxx
        String appTransId = generateAppTransId(request.getOrderCode());

        // Current timestamp in milliseconds
        long appTime = System.currentTimeMillis();

        // Prepare embed_data and item
        Map<String, Object> embedData = new HashMap<>();
        embedData.put("redirecturl", redirectUrl);

        List<Map<String, Object>> items = new ArrayList<>();
        Map<String, Object> item = new HashMap<>();
        item.put("itemid", request.getOrderCode());
        item.put("itemname", request.getDescription());
        item.put("itemprice", request.getAmount());
        item.put("itemquantity", 1);
        items.add(item);

        // Convert to JSON strings
        String embedDataJson = objectMapper.writeValueAsString(embedData);
        String itemJson = objectMapper.writeValueAsString(items);

        // Build order data
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("app_id", appId);
        orderData.put("app_user", request.getAppUser());
        orderData.put("app_time", appTime);
        orderData.put("amount", request.getAmount());
        orderData.put("app_trans_id", appTransId);
        orderData.put("embed_data", embedDataJson);
        orderData.put("item", itemJson);
        orderData.put("description", request.getDescription());
        orderData.put("bank_code", "");
        orderData.put("callback_url", callbackUrl);

        // Calculate MAC signature
        String macData = String.format(
            "%s|%s|%s|%s|%s|%s|%s",
            appId,
            appTransId,
            request.getAppUser(),
            request.getAmount(),
            appTime,
            embedDataJson,
            itemJson
        );
        String mac = calculateHmacSHA256(macData, key1);
        orderData.put("mac", mac);

        log.debug("ZaloPay order data: {}", orderData);

        // Send request to ZaloPay
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(orderData, headers);

        ResponseEntity<Map> response = restTemplate.exchange(endpoint, HttpMethod.POST, entity, Map.class);

        log.debug("ZaloPay response: {}", response.getBody());

        // Parse response
        ZaloPayOrderResponse orderResponse = new ZaloPayOrderResponse();
        Map<String, Object> responseBody = response.getBody();

        if (responseBody != null) {
            orderResponse.setReturnCode((Integer) responseBody.get("return_code"));
            orderResponse.setReturnMessage((String) responseBody.get("return_message"));
            orderResponse.setOrderUrl((String) responseBody.get("order_url"));
            orderResponse.setZpTransToken((String) responseBody.get("zp_trans_token"));
        }

        return orderResponse;
    }

    /**
     * Verify callback from ZaloPay
     */
    public boolean verifyCallback(String dataStr, String reqMac) {
        try {
            String mac = calculateHmacSHA256(dataStr, key2);
            return mac.equals(reqMac);
        } catch (Exception e) {
            log.error("Error verifying callback: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Generate app_trans_id with format: yyMMdd_xxxxx
     */
    private String generateAppTransId(String orderCode) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd");
        String dateStr = sdf.format(new Date());
        return dateStr + "_" + orderCode;
    }

    /**
     * Calculate HMAC SHA256
     */
    private String calculateHmacSHA256(String data, String key) throws Exception {
        Mac hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        hmac.init(secretKey);
        byte[] hash = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));

        // Convert to hex string
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
