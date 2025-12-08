package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Product entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, ProductRepositoryCustom {
    Optional<Product> findFirstByName(String name);

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category WHERE p.category.slug = :slug")
    Page<Product> findByCategory_Slug(Pageable pageable, @Param("slug") String slug);

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Product> findByNameContainingIgnoreCase(Pageable pageable, @Param("name") String name);

    @Query(
        "SELECT p FROM Product p LEFT JOIN FETCH p.category WHERE p.category.slug = :slug AND LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))"
    )
    Page<Product> findByCategory_SlugAndNameContainingIgnoreCase(Pageable pageable, @Param("slug") String slug, @Param("name") String name);
}
