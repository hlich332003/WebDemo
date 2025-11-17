package com.mycompany.myapp.repository;

import com.mycompany.myapp.web.rest.dto.TopSellingProductDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class ProductRepositoryCustomImpl implements ProductRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<TopSellingProductDTO> getTopSellingProducts(int topN) {
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("sp_GetTopSellingProducts");
        query.registerStoredProcedureParameter("TopN", Integer.class, ParameterMode.IN);
        query.setParameter("TopN", topN);

        query.execute();
        List<Object[]> results = query.getResultList();
        List<TopSellingProductDTO> dtos = new ArrayList<>();

        for (Object[] row : results) {
            TopSellingProductDTO dto = new TopSellingProductDTO();
            dto.setId(((Number) row[0]).longValue());
            dto.setName((String) row[1]);
            dto.setPrice(((Number) row[2]).doubleValue());
            dto.setImageUrl((String) row[3]);
            dto.setTotalOrders(((Number) row[4]).intValue());
            dto.setTotalQuantitySold(((Number) row[5]).intValue());
            dtos.add(dto);
        }

        return dtos;
    }

    @Override
    public int updateProductQuantity(Long productId, int quantity) {
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("sp_UpdateProductQuantity");
        query.registerStoredProcedureParameter("ProductId", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("Quantity", Integer.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("Result", Integer.class, ParameterMode.OUT);

        query.setParameter("ProductId", productId);
        query.setParameter("Quantity", quantity);
        query.execute();

        return (Integer) query.getOutputParameterValue("Result");
    }
}
