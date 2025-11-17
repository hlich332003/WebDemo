package com.mycompany.myapp.repository;

import com.mycompany.myapp.web.rest.dto.TopSellingProductDTO;
import java.util.List;

public interface ProductRepositoryCustom {
    List<TopSellingProductDTO> getTopSellingProducts(int topN);
    int updateProductQuantity(Long productId, int quantity);
}
