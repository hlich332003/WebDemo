package com.mycompany.myapp.repository;

import com.mycompany.myapp.web.rest.dto.OrderSearchDTO;
import java.util.List;

public interface OrderRepositoryCustom {
    List<OrderSearchDTO> searchOrders(String searchTerm);
}
