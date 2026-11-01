package com.shopsmart.service;

import com.shopsmart.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    @Mock ProductRepository products;
    @InjectMocks ProductService service;

    @Test
    void filtersBySearchAndCategoryCaseInsensitively() {
        Pageable pageable = PageRequest.of(0, 10);
        when(products.findByActiveTrueAndNameContainingIgnoreCaseAndCategoryIgnoreCase("mouse", "electronics", pageable))
                .thenReturn(new PageImpl<>(List.of()));

        assertNotNull(service.list(" mouse ", " electronics ", pageable));

        verify(products).findByActiveTrueAndNameContainingIgnoreCaseAndCategoryIgnoreCase("mouse", "electronics", pageable);
    }
}
