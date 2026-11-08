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
                    new Product("Scented Soy Candle", "A clean-burning soy candle with gentle cedar and citrus notes.", new BigDecimal("599.00"), 21, "Home", "https://picsum.photos/seed/shopsmart-candle/600/450")
            ));
        };
    }
}
