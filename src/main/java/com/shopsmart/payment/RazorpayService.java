package com.shopsmart.payment;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class RazorpayService {

    private final String keyId;
    private final String keySecret;

    public RazorpayService(
            @Value("${razorpay.key.id:}") String keyId,
            @Value("${razorpay.key.secret:}") String keySecret) {
        this.keyId = keyId;
        this.keySecret = keySecret;
    }

    public boolean isConfigured() {
        return !keyId.isBlank() && !keySecret.isBlank();
    }

    public String getKeyId() {
        return keyId;
    }

    // Creates a Razorpay order and returns its id (e.g. order_XXXXXXXX).
    public String createOrder(BigDecimal amount, Long shopSmartOrderId) {

        if (!isConfigured()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Razorpay is not configured on the server"
            );
        }

        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);

            JSONObject request = new JSONObject();
            request.put("amount", amount
                    .multiply(BigDecimal.valueOf(100))
                    .longValueExact());
            request.put("currency", "INR");
            request.put("receipt", "order_" + shopSmartOrderId);

            JSONObject notes = new JSONObject();
            notes.put("shopSmartOrderId", String.valueOf(shopSmartOrderId));
            request.put("notes", notes);

            Order order = client.orders.create(request);

            return order.get("id");
        } catch (RazorpayException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Razorpay order creation failed",
                    e
            );
        }
    }

    // HMAC-SHA256(razorpay_order_id + "|" + razorpay_payment_id, key_secret)
    public boolean verifySignature(
            String razorpayOrderId,
            String razorpayPaymentId,
            String signature) {

        if (!isConfigured()
                || razorpayOrderId == null
                || razorpayPaymentId == null
                || signature == null) {
            return false;
        }

        try {
            String payload = razorpayOrderId + "|" + razorpayPaymentId;

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    keySecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            ));

            byte[] hash = mac.doFinal(
                    payload.getBytes(StandardCharsets.UTF_8)
            );

            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }

            return MessageDigest.isEqual(
                    hex.toString().getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            return false;
        }
    }
}
