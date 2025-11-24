package com.mycompany.myapp.repository;

import com.mycompany.myapp.web.rest.dto.OrderSearchDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * Custom Repository Implementation với 2 cách ResultSet Mapping theo Baeldung:
 * 1. Tuple Query (đơn giản, linh hoạt) - Đang dùng
 * 2. @SqlResultSetMapping (chuẩn JPA, tái sử dụng) - Alternative
 *
 * Xem: https://www.baeldung.com/jpa-sql-resultset-mapping
 */
@Repository
public class OrderRepositoryCustomImpl implements OrderRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * CÁCH 1: Dùng Tuple Query (hiện đang dùng)
     * Ưu: Đơn giản, linh hoạt, dễ debug
     * Nhược: Phải map thủ công
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<OrderSearchDTO> searchOrders(String searchTerm) {
        String nativeQuery =
            """
            SELECT
                o.id,
                u.login AS customerLogin,
                o.order_date AS orderDate,
                o.total_amount AS totalAmount,
                o.status AS status
            FROM
                jhi_order o
            LEFT JOIN
                jhi_user u ON o.user_id = u.id
            WHERE
                LOWER(o.order_code) LIKE LOWER(:searchTermWithWildcard) OR
                LOWER(o.customer_full_name) LIKE LOWER(:searchTermWithWildcard) OR
                LOWER(u.login) LIKE LOWER(:searchTermWithWildcard)
            ORDER BY
                o.order_date DESC
            """;

        // Dùng Tuple query (chuẩn JPA) thay vì ResultSetMapping
        //noinspection JpaQueryApiInspection
        List<Tuple> tuples = entityManager
            .createNativeQuery(nativeQuery, Tuple.class)
            .setParameter("searchTermWithWildcard", "%" + searchTerm + "%")
            .getResultList();

        // Map Tuple sang DTO
        return tuples
            .stream()
            .map(tuple -> {
                // Convert BigDecimal sang Double
                BigDecimal totalAmount = tuple.get("totalAmount", BigDecimal.class);
                Double totalAmountDouble = (totalAmount != null) ? totalAmount.doubleValue() : null;

                return new OrderSearchDTO(
                    tuple.get("id", Long.class),
                    tuple.get("customerLogin", String.class),
                    tuple.get("orderDate", Instant.class),
                    totalAmountDouble,
                    tuple.get("status", String.class)
                );
            })
            .toList();
    }

    /**
     * CÁCH 2: Dùng @SqlResultSetMapping (định nghĩa trong Order.java)
     * Ưu: Chuẩn JPA, tái sử dụng, clean code
     * Nhược: Phải define mapping trước trong entity
     *
     * Theo: https://www.baeldung.com/jpa-sql-resultset-mapping
     *
     * NOTE: Không dùng method này trong production, chỉ để demo alternative approach
     */
    @SuppressWarnings("unchecked")
    public List<OrderSearchDTO> searchOrdersWithMapping(String searchTerm) {
        String nativeQuery =
            """
            SELECT
                o.id,
                u.login AS customerLogin,
                o.order_date AS orderDate,
                o.total_amount AS totalAmount,
                o.status AS status
            FROM
                jhi_order o
            LEFT JOIN
                jhi_user u ON o.user_id = u.id
            WHERE
                LOWER(o.order_code) LIKE LOWER(:searchTermWithWildcard) OR
                LOWER(o.customer_full_name) LIKE LOWER(:searchTermWithWildcard) OR
                LOWER(u.login) LIKE LOWER(:searchTermWithWildcard)
            ORDER BY
                o.order_date DESC
            """;

        // Dùng @SqlResultSetMapping đã define trong Order.java
        //noinspection JpaQueryApiInspection
        return entityManager
            .createNativeQuery(nativeQuery, "OrderSearchDTOMapping")
            .setParameter("searchTermWithWildcard", "%" + searchTerm + "%")
            .getResultList();
    }
}
