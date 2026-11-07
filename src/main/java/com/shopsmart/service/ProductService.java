
package com.shopsmart.service;

import com.shopsmart.api.ApiDtos.ProductRequest;
import com.shopsmart.product.Product;
import com.shopsmart.product.ProductRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductService {

    private final ProductRepository products;

    public ProductService(ProductRepository products) {
        this.products = products;
    }

    public Page<Product> list(
            String search,
            String category,
            Pageable pageable) {

        boolean hasSearch = search != null && !search.isBlank();
        boolean hasCategory = category != null && !category.isBlank();

        if (hasSearch && hasCategory) {
            return products.findByActiveTrueAndNameContainingIgnoreCaseAndCategoryIgnoreCase(
                    search.trim(), category.trim(), pageable);
        }

        if (hasSearch) {
            return products.findByActiveTrueAndNameContainingIgnoreCase(
                    search.trim(), pageable);
        }

        if (hasCategory) {
            return products.findByActiveTrueAndCategoryIgnoreCase(
                    category.trim(), pageable);
        }

        return products.findByActiveTrue(pageable);
    }

    public java.util.List<String> categories() {
        return products.findActiveCategories();
    }

    public Product get(Long id) {
        return products.findById(id)
                .filter(Product::isActive)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Product not found"));
    }

    // Create product with image URL
    public Product create(ProductRequest r) {

        Product product = new Product(
                r.name().trim(),
                r.description().trim(),
                r.price(),
                r.stock(),
                r.category().trim(),
                r.imageUrl()
        );

        return products.save(product);
    }

    // Update product including image URL
    public Product update(Long id, ProductRequest r) {

        Product p = products.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Product not found"));

        p.update(
                r.name().trim(),
                r.description().trim(),
                r.price(),
                r.stock(),
                r.category().trim(),
                r.imageUrl()
        );

        return products.save(p);
    }

    public void delete(Long id) {

        Product p = get(id);
        p.deactivate();
        products.save(p);
    }
}
