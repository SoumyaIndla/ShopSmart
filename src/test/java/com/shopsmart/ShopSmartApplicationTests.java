package com.shopsmart;

import com.jayway.jsonpath.JsonPath;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ShopSmartApplicationTests {
    @Autowired MockMvc mvc;
    @Autowired ProductRepository products;

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
