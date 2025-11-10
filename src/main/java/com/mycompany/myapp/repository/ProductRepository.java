package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Product entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByName(String name);
    List<Product> findByIsFeaturedTrue();

    Page<Product> findByCategory_Slug(Pageable pageable, String categorySlug);
    Page<Product> findByNameContainingIgnoreCase(Pageable pageable, String name);
    Page<Product> findByCategory_SlugAndNameContainingIgnoreCase(Pageable pageable, String categorySlug, String name);
}
