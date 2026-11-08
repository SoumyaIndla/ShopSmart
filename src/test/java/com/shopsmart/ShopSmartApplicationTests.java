package com.shopsmart;

import com.jayway.jsonpath.JsonPath;
import com.shopsmart.payment.PaymentRepository;
import com.shopsmart.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ShopSmartApplicationTests {
    @Autowired MockMvc mvc;
    @Autowired ProductRepository products;
    @Autowired PaymentRepository payments;

    @Test
    void storefrontIsPubliclyAvailable() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk());
    }

    @Test
    void customerCanRegisterAddToCartAndCheckoutWithStockUpdated() throws Exception {
        String adminToken = login("admin@shopsmart.test", "TestAdmin#2026");

        String productJson = mvc.perform(post("/api/products")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Test Mouse","description":"Wireless test mouse","price":25.50,"stock":4,"category":"Electronics"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andReturn().getResponse().getContentAsString();
        Long productId = ((Number) JsonPath.read(productJson, "$.id")).longValue();

        String email = "customer-" + System.nanoTime() + "@example.test";
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Test Customer","email":"%s","password":"CustomerPass#2026"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("CUSTOMER"));

        String customerToken = login(email, "CustomerPass#2026");
        mvc.perform(post("/api/cart/items")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + productId + ",\"quantity\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(2));

        mvc.perform(post("/api/orders").header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.total").value(51.0))
                .andExpect(jsonPath("$.items[0].quantity").value(2));

        assertEquals(2, products.findById(productId).orElseThrow().getStock());
        mvc.perform(get("/api/cart").header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void customerCannotCreateAdminOnlyProduct() throws Exception {
        String email = "customer-" + System.nanoTime() + "@example.test";
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Test Customer","email":"%s","password":"CustomerPass#2026"}
                                """.formatted(email)))
                .andExpect(status().isOk());
        String token = login(email, "CustomerPass#2026");

        mvc.perform(post("/api/products")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Forbidden Item","description":"Should not be created","price":5.00,"stock":1,"category":"Test"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void checkoutRejectsInsufficientStockAndKeepsTheCart() throws Exception {
        String adminToken = login("admin@shopsmart.test", "TestAdmin#2026");
        String productJson = mvc.perform(post("/api/products")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Limited Item","description":"Stock race test","price":10.00,"stock":4,"category":"Test"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long productId = ((Number) JsonPath.read(productJson, "$.id")).longValue();

        String email = "stock-test-" + System.nanoTime() + "@example.test";
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Stock Test","email":"%s","password":"CustomerPass#2026"}
                                """.formatted(email)))
                .andExpect(status().isOk());
        String customerToken = login(email, "CustomerPass#2026");
        mvc.perform(post("/api/cart/items")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + productId + ",\"quantity\":2}"))
                .andExpect(status().isOk());

        var product = products.findById(productId).orElseThrow();
        product.update(product.getName(), product.getDescription(), product.getPrice(), 1, product.getCategory());
        products.save(product);

        mvc.perform(post("/api/orders").header("Authorization", bearer(customerToken)))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/cart").header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(2));
        assertEquals(1, products.findById(productId).orElseThrow().getStock());
    }

    @Test
    void cartQuantityCanBeUpdatedAndOrderCancellationRestoresStock() throws Exception {
        String adminToken = login("admin@shopsmart.test", "TestAdmin#2026");
        String productJson = mvc.perform(post("/api/products")
                        .header("Authorization", bearer(adminToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Cancel Test Item","description":"Restock after cancellation","price":12.00,"stock":5,"category":"Test"}
                                """))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Long productId = ((Number) JsonPath.read(productJson, "$.id")).longValue();

        String email = "cancel-test-" + System.nanoTime() + "@example.test";
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Cancel Test","email":"%s","password":"CustomerPass#2026"}
                                """.formatted(email)))
                .andExpect(status().isOk());
        String customerToken = login(email, "CustomerPass#2026");

        mvc.perform(post("/api/cart/items").header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + productId + ",\"quantity\":1}"))
                .andExpect(status().isOk());
        mvc.perform(put("/api/cart/items/" + productId).header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":3}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].quantity").value(3));

        String orderJson = mvc.perform(post("/api/orders").header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PLACED"))
                .andReturn().getResponse().getContentAsString();
        Long orderId = ((Number) JsonPath.read(orderJson, "$.id")).longValue();
        assertEquals(2, products.findById(productId).orElseThrow().getStock());

        mvc.perform(post("/api/orders/" + orderId + "/cancel").header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        assertEquals(5, products.findById(productId).orElseThrow().getStock());
        mvc.perform(post("/api/orders/" + orderId + "/cancel").header("Authorization", bearer(customerToken)))
                .andExpect(status().isConflict());
    }

    @Test
    void productBrowseSupportsSearchCategoryAndPagination() throws Exception {
        String adminToken = login("admin@shopsmart.test", "TestAdmin#2026");
        mvc.perform(post("/api/products").header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Filtered Keyboard","description":"Searchable item","price":35.00,"stock":8,"category":"Accessories"}
                                """))
                .andExpect(status().isOk());

        mvc.perform(get("/api/products?search=keyboard&category=accessories&page=0&size=1&sortBy=price&direction=desc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Filtered Keyboard"));
    }

    @Test
    void logoutInvalidatesTheCurrentJwt() throws Exception {
        String email = "logout-test-" + System.nanoTime() + "@example.test";
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Logout Test","email":"%s","password":"CustomerPass#2026"}
                                """.formatted(email)))
                .andExpect(status().isOk());
        String token = login(email, "CustomerPass#2026");

        mvc.perform(post("/api/auth/logout").header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/cart").header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void incorrectPasswordIsRejectedAndOrderIsPrivateToItsOwner() throws Exception {
        String email = "private-order-" + System.nanoTime() + "@example.test";
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Private Order","email":"%s","password":"CustomerPass#2026"}
                                """.formatted(email)))
                .andExpect(status().isOk());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"WrongPassword#2026"}
                                """.formatted(email)))
                .andExpect(status().isUnauthorized());

        String adminToken = login("admin@shopsmart.test", "TestAdmin#2026");
        String productJson = mvc.perform(post("/api/products").header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Private Order Item","description":"Ownership test","price":8.00,"stock":4,"category":"Test"}
                                """))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Long productId = ((Number) JsonPath.read(productJson, "$.id")).longValue();

        String ownerToken = login(email, "CustomerPass#2026");
        mvc.perform(post("/api/cart/items").header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"productId\":" + productId + ",\"quantity\":1}"))
                .andExpect(status().isOk());
        String orderJson = mvc.perform(post("/api/orders").header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Long orderId = ((Number) JsonPath.read(orderJson, "$.id")).longValue();

        String otherEmail = "other-user-" + System.nanoTime() + "@example.test";
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Other User","email":"%s","password":"CustomerPass#2026"}
                                """.formatted(otherEmail)))
                .andExpect(status().isOk());
        String otherToken = login(otherEmail, "CustomerPass#2026");
        mvc.perform(get("/api/orders/" + orderId).header("Authorization", bearer(otherToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidCheckoutPaymentMethodIsRejectedWithBadRequest() throws Exception {
        String token = registerAndLogin("invalid-method");

        mvc.perform(post("/api/orders")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\":\"BITCOIN\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownRouteReturnsNotFoundInsteadOfServerError() throws Exception {
        String token = registerAndLogin("unknown-route");

        mvc.perform(get("/api/does-not-exist")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void nonJsonContentTypeIsRejectedWithUnsupportedMediaType() throws Exception {
        String token = registerAndLogin("media-type");

        mvc.perform(post("/api/orders")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .content("paymentMethod=COD"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void adminUpdatesDeliveryStatusAndOwnerCanTrackIt() throws Exception {
        String adminToken = login("admin@shopsmart.test", "TestAdmin#2026");
        String customerToken = registerAndLogin("delivery");
        Long orderId = createOrder(customerToken, "Delivery Test Item");

        // New orders start at PROCESSING.
        mvc.perform(get("/api/orders/" + orderId + "/tracking")
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryStatus").value("PROCESSING"))
                .andExpect(jsonPath("$.timeline[0].current").value(true));

        // A customer cannot move the delivery status.
        mvc.perform(patch("/api/admin/orders/" + orderId + "/delivery-status")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deliveryStatus\":\"SHIPPED\"}"))
                .andExpect(status().isForbidden());

        // An admin can, and the order status follows the delivery status.
        mvc.perform(patch("/api/admin/orders/" + orderId + "/delivery-status")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deliveryStatus\":\"SHIPPED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryStatus").value("SHIPPED"))
                .andExpect(jsonPath("$.status").value("SHIPPED"));

        // The owner sees the updated timeline (index 3 = SHIPPED).
        mvc.perform(get("/api/orders/" + orderId + "/tracking")
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryStatus").value("SHIPPED"))
                .andExpect(jsonPath("$.timeline[3].completed").value(true))
                .andExpect(jsonPath("$.timeline[4].completed").value(false));

        // Another customer cannot track it.
        String otherToken = registerAndLogin("other-delivery");
        mvc.perform(get("/api/orders/" + orderId + "/tracking")
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isNotFound());

        // An unknown status is a bad request, not a server error.
        mvc.perform(patch("/api/admin/orders/" + orderId + "/delivery-status")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deliveryStatus\":\"TELEPORTED\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void codPaymentIsOwnerScopedAndAdminConfirmed() throws Exception {
        String adminToken = login("admin@shopsmart.test", "TestAdmin#2026");
        String customerToken = registerAndLogin("payment");
        Long orderId = createOrder(customerToken, "Payment Test Item");

        // Owner can read their payment; it starts PENDING.
        mvc.perform(get("/api/payments/order/" + orderId)
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method").value("COD"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        // Another customer gets 404, not someone else's payment.
        String otherToken = registerAndLogin("other-payment");
        mvc.perform(get("/api/payments/order/" + orderId)
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isNotFound());

        // A customer cannot confirm COD.
        mvc.perform(post("/api/payments/cod/" + orderId + "/confirm")
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isForbidden());

        // An admin can confirm it exactly once.
        mvc.perform(post("/api/payments/cod/" + orderId + "/confirm")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        mvc.perform(get("/api/payments/order/" + orderId)
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        mvc.perform(post("/api/payments/cod/" + orderId + "/confirm")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isConflict());
    }

    @Test
    void razorpaySignatureMarksPaymentSuccessful() throws Exception {
        String token = registerAndLogin("razorpay-ok");
        Long orderId = createOrder(token, "Razorpay OK Item", "RAZORPAY");

        var payment = payments.findByOrderId(orderId).orElseThrow();
        payment.setRazorpayOrderId("order_test_ok");
        payments.save(payment);

        String signature = hmacHex(
                "order_test_ok|pay_test_ok",
                "shopsmart-test-razorpay-secret"
        );

        mvc.perform(post("/api/payments/razorpay/verify")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"razorpayOrderId":"order_test_ok","razorpayPaymentId":"pay_test_ok","razorpaySignature":"%s"}
                                """.formatted(signature)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method").value("RAZORPAY"))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.razorpayPaymentId").value("pay_test_ok"));
    }

    @Test
    void razorpayWithBadSignatureIsRejected() throws Exception {
        String token = registerAndLogin("razorpay-bad");
        Long orderId = createOrder(token, "Razorpay Bad Item", "RAZORPAY");

        var payment = payments.findByOrderId(orderId).orElseThrow();
        payment.setRazorpayOrderId("order_test_bad");
        payments.save(payment);

        mvc.perform(post("/api/payments/razorpay/verify")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"razorpayOrderId":"order_test_bad","razorpayPaymentId":"pay_test_bad","razorpaySignature":"deadbeef"}
                                """))
                .andExpect(status().isBadRequest());
    }

    private String hmacHex(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        ));

        byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();

        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }

        return hex.toString();
    }

    private String registerAndLogin(String prefix) throws Exception {
        String email = prefix + "-" + System.nanoTime() + "@example.test";

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Test User","email":"%s","password":"CustomerPass#2026"}
                                """.formatted(email)))
                .andExpect(status().isOk());

        return login(email, "CustomerPass#2026");
    }

    private Long createOrder(String customerToken, String productName) throws Exception {
        return createOrder(customerToken, productName, "COD");
    }

    private Long createOrder(
            String customerToken,
            String productName,
            String paymentMethod) throws Exception {

        String adminToken = login("admin@shopsmart.test", "TestAdmin#2026");

        String productJson = mvc.perform(post("/api/products")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","description":"test product","price":10.00,"stock":5,"category":"Test"}
                                """.formatted(productName)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long productId = ((Number) JsonPath.read(productJson, "$.id")).longValue();

        mvc.perform(post("/api/cart/items")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + productId + ",\"quantity\":1}"))
                .andExpect(status().isOk());

        String orderJson = mvc.perform(post("/api/orders")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\":\"" + paymentMethod + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return ((Number) JsonPath.read(orderJson, "$.id")).longValue();
    }

    private String login(String email, String password) throws Exception {
        String response = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.token");
    }

    private String bearer(String token) { return "Bearer " + token; }
}
