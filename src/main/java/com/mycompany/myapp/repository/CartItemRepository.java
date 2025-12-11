package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.CartItem;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
}
