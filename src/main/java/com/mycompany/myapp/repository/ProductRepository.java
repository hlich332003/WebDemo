package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.Product;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    Optional<Product> findFirstByName(String name);

    // Các phương thức cũ vẫn có thể giữ lại nếu cần
    Page<Product> findByCategory_SlugAndNameContainingIgnoreCase(Pageable pageable, String categorySlug, String name);
    Page<Product> findByCategory_Slug(Pageable pageable, String categorySlug);
    Page<Product> findByNameContainingIgnoreCase(Pageable pageable, String name);
}
