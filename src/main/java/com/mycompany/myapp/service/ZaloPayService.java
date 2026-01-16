package com.mycompany.myapp.service;

import com.mycompany.myapp.config.ZaloPayProperties;
import com.mycompany.myapp.domain.Order;
import com.mycompany.myapp.domain.PaymentTransaction;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.repository.OrderRepository;
import com.mycompany.myapp.repository.PaymentTransactionRepository;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ZaloPayService {

    private static final Logger log = LoggerFactory.getLogger(ZaloPayService.class);
    private static final int CONNECTION_TIMEOUT = 10000; // 10 seconds
    private static final int SOCKET_TIMEOUT = 10000; // 10 seconds

    private final ZaloPayProperties zalopayConfig;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final OrderRepository orderRepository;

    public ZaloPayService(
        ZaloPayProperties zalopayConfig,
        PaymentTransactionRepository paymentTransactionRepository,
        OrderRepository orderRepository
    ) {
        this.zalopayConfig = zalopayConfig;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.orderRepository = orderRepository;
    }

    public Map<String, Object> createOrder(Long orderId) {
        log.info("Creating ZaloPay order for Order ID: {}", orderId);
        Map<String, Object> result = new HashMap<>();

        Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng ID: " + orderId));

        long amount = order.getTotalAmount().longValue();

        // Lấy thông tin user mua hàng để gửi sang ZaloPay (thay vì fix cứng user123)
        String appUser = order.getCustomerEmail();
        if (appUser == null || appUser.isEmpty()) {
            appUser = order.getCustomerFullName();
        }
        if (appUser == null || appUser.isEmpty()) {
            appUser = "Guest";
        }

        String appTransId = new SimpleDateFormat("yyMMdd").format(new Date()) + "_" + orderId + "_" + System.currentTimeMillis();
        long appTime = System.currentTimeMillis();

        JSONObject embedData = new JSONObject();
        embedData.put("redirecturl", zalopayConfig.getRedirectUrl());
        embedData.put("orderId", orderId);
        String embedDataStr = embedData.toString(); // Lưu thành chuỗi để đảm bảo nhất quán

        String item = "[]";

        // Tính MAC
        String data =
            zalopayConfig.getAppid() + "|" + appTransId + "|" + appUser + "|" + amount + "|" + appTime + "|" + embedDataStr + "|" + item;
        String mac = new HmacUtils("HmacSHA256", zalopayConfig.getKey1()).hmacHex(data);

        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("app_id", zalopayConfig.getAppid()));
        params.add(new BasicNameValuePair("app_user", appUser));
        params.add(new BasicNameValuePair("app_time", String.valueOf(appTime)));
        params.add(new BasicNameValuePair("amount", String.valueOf(amount)));
        params.add(new BasicNameValuePair("app_trans_id", appTransId));
        params.add(new BasicNameValuePair("embed_data", embedDataStr));
        params.add(new BasicNameValuePair("item", item));
        params.add(new BasicNameValuePair("description", "Thanh toan don hang #" + orderId));
        params.add(new BasicNameValuePair("bank_code", ""));
        params.add(new BasicNameValuePair("callback_url", zalopayConfig.getCallbackUrl()));
        params.add(new BasicNameValuePair("mac", mac));

        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(CONNECTION_TIMEOUT).setSocketTimeout(SOCKET_TIMEOUT).build();

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(zalopayConfig.getEndpoint());
            post.setConfig(requestConfig);
            post.setEntity(new UrlEncodedFormEntity(params));

            try (CloseableHttpResponse res = client.execute(post)) {
                BufferedReader rd = new BufferedReader(new InputStreamReader(res.getEntity().getContent()));
                StringBuilder resultJsonStr = new StringBuilder();
                String line;
                while ((line = rd.readLine()) != null) {
                    resultJsonStr.append(line);
                }
                JSONObject resultJson = new JSONObject(resultJsonStr.toString());

                log.info("ZaloPay response: {}", resultJson);

                if (resultJson.getInt("return_code") == 1) {
                    PaymentTransaction trans = new PaymentTransaction();
                    trans.setOrder(order);
                    trans.setAmount(BigDecimal.valueOf(amount));
                    trans.setAppTransId(appTransId);
                    trans.setStatus("PENDING");
                    trans.setCreatedAt(Instant.now());
                    trans.setDescription("Cho thanh toan qua ZaloPay");
                    trans.setPaymentUrl(resultJson.optString("order_url"));

                    paymentTransactionRepository.save(trans);

                    result.put("order_url", resultJson.get("order_url"));
                    result.put("qr_code_url", resultJson.optString("qr_code_url", ""));
                    result.put("app_trans_id", appTransId);
                    log.info("Payment transaction created successfully for Order ID: {}", orderId);
                } else {
                    log.error("ZaloPay order creation failed with return_code: {}", resultJson.getInt("return_code"));
                }

                result.put("return_code", resultJson.get("return_code"));
                result.put("return_message", resultJson.get("return_message"));
            }
        } catch (Exception e) {
            log.error("Error creating ZaloPay order", e);
            throw new RuntimeException("Lỗi khi tạo đơn hàng ZaloPay: " + e.getMessage());
        }

        return result;
    }

    public JSONObject verifyCallback(String jsonStr) {
        log.info("Verifying ZaloPay callback");
        JSONObject result = new JSONObject();
        try {
            JSONObject cbdata = new JSONObject(jsonStr);
            String dataStr = cbdata.getString("data");
            String reqMac = cbdata.getString("mac");

            String mac = new HmacUtils("HmacSHA256", zalopayConfig.getKey2()).hmacHex(dataStr);

            if (!mac.equals(reqMac)) {
                log.error("MAC verification failed");
                result.put("return_code", -1);
                result.put("return_message", "mac not equal");
                return result;
            }

            JSONObject dataJson = new JSONObject(dataStr);
            String appTransId = dataJson.getString("app_trans_id");
            String zpTransId = dataJson.getString("zp_trans_id");

            log.info("Processing callback for app_trans_id: {}", appTransId);

            Optional<PaymentTransaction> transOpt = paymentTransactionRepository.findByAppTransId(appTransId);

            if (transOpt.isPresent()) {
                PaymentTransaction trans = transOpt.get();
                if (!"SUCCESS".equals(trans.getStatus())) {
                    trans.setStatus("SUCCESS");
                    trans.setZpTransId(zpTransId);
                    trans.setPaidAt(Instant.now());
                    paymentTransactionRepository.save(trans);

                    Order order = trans.getOrder();
                    if (order != null) {
                        order.setStatus(OrderStatus.PAID);
                        orderRepository.save(order);
                        log.info("Order ID: {} marked as PAID", order.getId());
                    }
                }
            } else {
                log.warn("Payment transaction not found for app_trans_id: {}", appTransId);
            }

            result.put("return_code", 1);
            result.put("return_message", "success");
        } catch (Exception e) {
            log.error("Error verifying callback", e);
            result.put("return_code", 0);
            result.put("return_message", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> checkPaymentStatus(String appTransId) {
        log.info("Checking payment status for app_trans_id: {}", appTransId);
        Map<String, Object> result = new HashMap<>();

        try {
            Optional<PaymentTransaction> transOpt = paymentTransactionRepository.findByAppTransId(appTransId);

            if (transOpt.isPresent()) {
                PaymentTransaction trans = transOpt.get();
                result.put("status", trans.getStatus());
                result.put("amount", trans.getAmount());
                result.put("created_at", trans.getCreatedAt());
                result.put("paid_at", trans.getPaidAt());
                result.put("zp_trans_id", trans.getZpTransId());
                result.put("return_code", 1);
            } else {
                result.put("return_code", -1);
                result.put("return_message", "Transaction not found");
            }
        } catch (Exception e) {
            log.error("Error checking payment status", e);
            result.put("return_code", 0);
            result.put("return_message", e.getMessage());
        }

        return result;
    }
}
