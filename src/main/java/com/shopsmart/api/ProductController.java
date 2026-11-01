package com.shopsmart.api;

import com.shopsmart.api.ApiDtos.ProductRequest;
import com.shopsmart.api.ApiDtos.ProductResponse;
import com.shopsmart.api.ApiDtos.PageResponse;
import com.shopsmart.service.ProductService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private static final Set<String> SORT_FIELDS = Set.of("name", "price", "category", "stock");
    private final ProductService service;

    public ProductController(ProductService service) { this.service = service; }

    @GetMapping("/categories") public java.util.List<String> categories() { return service.categories(); }

    @GetMapping
    public PageResponse<ProductResponse> list(@RequestParam(required = false) String search,
                                      @RequestParam(required = false) String category,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size,
                                      @RequestParam(defaultValue = "name") String sortBy,
                                      @RequestParam(defaultValue = "asc") String direction) {
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("page must be non-negative and size must be between 1 and 100");
        if (!SORT_FIELDS.contains(sortBy)) throw new IllegalArgumentException("Unsupported sortBy field");
        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        Page<ProductResponse> results = service.list(search, category, PageRequest.of(page, size, sort)).map(ApiMapper::product);
        return new PageResponse<>(results.getContent(), results.getNumber(), results.getSize(), results.getTotalElements(), results.getTotalPages(), results.isFirst(), results.isLast());
    }

    @GetMapping("/{id}") public ProductResponse get(@PathVariable Long id) { return ApiMapper.product(service.get(id)); }
    @PostMapping @PreAuthorize("hasRole('ADMIN')") @SecurityRequirement(name="bearerAuth") public ProductResponse create(@Valid @RequestBody ProductRequest r) { return ApiMapper.product(service.create(r)); }
    @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") @SecurityRequirement(name="bearerAuth") public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest r) { return ApiMapper.product(service.update(id, r)); }
    @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") @SecurityRequirement(name="bearerAuth") public void delete(@PathVariable Long id) { service.delete(id); }
}
