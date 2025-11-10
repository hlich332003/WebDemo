package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.Category;
import java.util.List; // Import List
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Category entity.
 */
@SuppressWarnings("unused")
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByName(String name);
    List<Category> findByIsFeaturedTrue(); // Thêm phương thức này
}
