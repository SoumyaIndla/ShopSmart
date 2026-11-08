package com.shopsmart.product;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository extends JpaRepository<Product,Long> {
    Page<Product> findByActiveTrue(Pageable pageable);
    Page<Product> findByActiveTrueAndNameContainingIgnoreCase(String name, Pageable pageable);
    Page<Product> findByActiveTrueAndCategoryIgnoreCase(String category, Pageable pageable);
    Page<Product> findByActiveTrueAndNameContainingIgnoreCaseAndCategoryIgnoreCase(String name, String category, Pageable pageable);
    @Query("select distinct p.category from Product p where p.active = true order by p.category")
    List<String> findActiveCategories();

    /*
     * Native FOR UPDATE avoids Hibernate's "FOR UPDATE OF alias" syntax,
     * which MySQL supports but TiDB does not.
     */
    @Query(value = "select * from products where id = :id and active = true for update", nativeQuery = true)
    Optional<Product> findActiveByIdForUpdate(@Param("id") Long id);

    @Query(value = "select * from products where id = :id for update", nativeQuery = true)
    Optional<Product> findByIdForUpdate(@Param("id") Long id);
}
