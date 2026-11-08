package com.shopsmart.product;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;
import java.util.List;

@Configuration
@Profile("!test")
public class DemoCatalogConfiguration {
    @Bean
    CommandLineRunner seedDemoCatalog(ProductRepository products,
                                      @Value("${shopsmart.demo-data.enabled:true}") boolean enabled) {
        return args -> {
            if (!enabled || products.count() != 0) return;
            products.saveAll(List.of(
                    new Product("Pour Over Coffee Set", "A ceramic dripper and matching cup for a slower morning ritual.", new BigDecimal("1299.00"), 18, "Kitchen", "https://picsum.photos/seed/shopsmart-coffee/600/450"),
                    new Product("Everyday Linen Tote", "A sturdy natural linen bag with a roomy interior and inner pocket.", new BigDecimal("899.00"), 26, "Accessories", "https://picsum.photos/seed/shopsmart-tote/600/450"),
                    new Product("Moss Desk Lamp", "A warm, adjustable desk lamp with a compact brushed-metal base.", new BigDecimal("2499.00"), 9, "Home", "https://picsum.photos/seed/shopsmart-lamp/600/450"),
                    new Product("Insulated Steel Bottle", "A double-wall bottle that keeps drinks cold throughout the day.", new BigDecimal("749.00"), 32, "Outdoors", "https://picsum.photos/seed/shopsmart-bottle/600/450"),
                    new Product("Soft Cotton Throw", "A breathable woven cotton throw for the sofa or the foot of the bed.", new BigDecimal("1899.00"), 12, "Home", "https://picsum.photos/seed/shopsmart-throw/600/450"),
                    new Product("Pocket Notebook Set", "Three recycled-paper notebooks for notes, lists, and good ideas.", new BigDecimal("399.00"), 40, "Stationery", "https://picsum.photos/seed/shopsmart-notes/600/450"),
                    new Product("Wireless Travel Mouse", "A quiet, rechargeable mouse designed to work wherever you do.", new BigDecimal("1199.00"), 15, "Electronics", "https://picsum.photos/seed/shopsmart-mouse/600/450"),
                    new Product("Scented Soy Candle", "A clean-burning soy candle with gentle cedar and citrus notes.", new BigDecimal("599.00"), 21, "Home", "https://picsum.photos/seed/shopsmart-candle/600/450"),
                    new Product("Ceramic Mug Set", "Four hand-glazed stoneware mugs for coffee, tea and everything between.", new BigDecimal("649.00"), 25, "Kitchen", "https://picsum.photos/seed/shopsmart-mugs/600/450"),
                    new Product("Chef Knife", "A balanced stainless-steel chef knife with a full-tang handle.", new BigDecimal("1299.00"), 12, "Kitchen", "https://picsum.photos/seed/shopsmart-knife/600/450"),
                    new Product("Bamboo Cutting Board", "Durable bamboo board with a juice groove and easy-grip handle.", new BigDecimal("899.00"), 18, "Kitchen", "https://picsum.photos/seed/shopsmart-board/600/450"),
                    new Product("Aroma Diffuser", "Ultrasonic diffuser with warm light and whisper-quiet mist.", new BigDecimal("1599.00"), 10, "Home", "https://picsum.photos/seed/shopsmart-diffuser/600/450"),
                    new Product("Cotton Bed Sheet Set", "Breathable queen-size cotton sheet set with two pillow covers.", new BigDecimal("1899.00"), 14, "Home", "https://picsum.photos/seed/shopsmart-bedsheet/600/450"),
                    new Product("Minimal Wall Clock", "A quiet sweep-movement clock with a clean wooden face.", new BigDecimal("1099.00"), 20, "Home", "https://picsum.photos/seed/shopsmart-clock/600/450"),
                    new Product("Bluetooth Earbuds", "Compact true-wireless earbuds with a pocket charging case.", new BigDecimal("2499.00"), 16, "Electronics", "https://picsum.photos/seed/shopsmart-earbuds/600/450"),
                    new Product("USB-C Hub", "Seven-in-one hub with HDMI, USB 3.0 and card readers.", new BigDecimal("1799.00"), 22, "Electronics", "https://picsum.photos/seed/shopsmart-hub/600/450"),
                    new Product("Smart Watch Band", "Soft silicone band that fits most smart watches.", new BigDecimal("799.00"), 30, "Electronics", "https://picsum.photos/seed/shopsmart-band/600/450"),
                    new Product("Yoga Mat", "Non-slip six-millimetre mat with a carry strap.", new BigDecimal("999.00"), 24, "Fitness", "https://picsum.photos/seed/shopsmart-yoga/600/450"),
                    new Product("Resistance Bands", "Set of five latex bands for strength and mobility work.", new BigDecimal("599.00"), 35, "Fitness", "https://picsum.photos/seed/shopsmart-bands/600/450"),
                    new Product("Leather Wallet", "Slim full-grain leather wallet with six card slots.", new BigDecimal("899.00"), 19, "Fashion", "https://picsum.photos/seed/shopsmart-wallet/600/450"),
                    new Product("Canvas Backpack", "Water-resistant canvas daypack with a padded laptop sleeve.", new BigDecimal("2199.00"), 11, "Fashion", "https://picsum.photos/seed/shopsmart-backpack/600/450"),
                    new Product("Hardcover Journal", "A linen-bound journal with 160 pages of thick cream paper.", new BigDecimal("499.00"), 40, "Stationery", "https://picsum.photos/seed/shopsmart-journal/600/450"),
                    new Product("Travel Umbrella", "Wind-resistant compact umbrella with an automatic open.", new BigDecimal("749.00"), 27, "Outdoors", "https://picsum.photos/seed/shopsmart-umbrella/600/450")
            ));
        };
    }
}
