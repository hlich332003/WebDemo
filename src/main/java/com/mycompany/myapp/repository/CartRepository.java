package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.Cart;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findOneByUser_Email(String email);
}
