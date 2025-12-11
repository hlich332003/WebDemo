package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.WishlistItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {
    List<WishlistItem> findByUser_Email(String email);

    Optional<WishlistItem> findOneByUser_EmailAndProductId(String email, Long productId);

    void deleteByUser_EmailAndProductId(String email, Long productId);
}
