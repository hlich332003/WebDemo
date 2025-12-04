package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.Order;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Order entity.
 */
@SuppressWarnings("unused")
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    @Query("SELECT o FROM Order o WHERE lower(o.customer.email) = lower(:identifier) OR o.customer.phone = :identifier ORDER BY o.orderDate DESC")
    List<Order> findByCustomerIdentifierOrderByOrderDateDesc(@Param("identifier") String identifier);

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.customer LEFT JOIN FETCH o.items")
    Page<Order> findAllOrders(Pageable pageable);

    Page<Order> findByOrderCodeContainingIgnoreCase(String orderCode, Pageable pageable);
}
