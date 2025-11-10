package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.Order;
import com.mycompany.myapp.domain.OrderItem;
import com.mycompany.myapp.domain.Product;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.domain.enumeration.OrderStatus; // Import OrderStatus
import com.mycompany.myapp.repository.OrderItemRepository;
import com.mycompany.myapp.repository.OrderRepository;
import com.mycompany.myapp.repository.ProductRepository;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.security.SecurityUtils;
import com.mycompany.myapp.service.dto.OrderDTO;
import com.mycompany.myapp.service.dto.OrderItemDTO;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrderService {

    private final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public OrderService(
        OrderRepository orderRepository,
        OrderItemRepository orderItemRepository,
        ProductRepository productRepository,
        UserRepository userRepository
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    /**
     * Save an order.
     *
     * @param order the entity to save.
     * @return the persisted entity.
     */
    public Order save(Order order) {
        log.debug("Request to save Order : {}", order);
        return orderRepository.save(order);
    }

    public Order create(OrderDTO orderDTO) {
        Order order = new Order();
        order.setOrderDate(Instant.now());
        order.setTotalAmount(orderDTO.getTotalAmount());
        order.setStatus(OrderStatus.PENDING); // Đặt trạng thái ban đầu bằng Enum

        // Lấy thông tin người dùng hiện tại và thiết lập cho đơn hàng nếu có
        SecurityUtils.getCurrentUserLogin().flatMap(userRepository::findOneByLogin).ifPresent(order::setCustomer);

        // Điền thông tin khách hàng từ DTO
        if (orderDTO.getCustomerInfo() != null) {
            order.setCustomerFullName(orderDTO.getCustomerInfo().getFullName());
            order.setCustomerEmail(orderDTO.getCustomerInfo().getEmail());
            order.setCustomerPhone(orderDTO.getCustomerInfo().getPhone());
            order.setDeliveryAddress(orderDTO.getCustomerInfo().getAddress());
            order.setPaymentMethod(orderDTO.getCustomerInfo().getPaymentMethod());
        }

        // Lưu đơn hàng trước để có ID cho OrderItem
        Order savedOrder = orderRepository.save(order);

        Set<OrderItem> orderItems = new HashSet<>();
        for (OrderItemDTO itemDTO : orderDTO.getItems()) {
            // Kiểm tra sản phẩm và số lượng tồn kho
            Optional<Product> productOptional = productRepository.findById(itemDTO.getProductId());
            if (productOptional.isEmpty()) {
                throw new BadRequestAlertException("Product with ID " + itemDTO.getProductId() + " not found", "order", "productNotFound");
            }
            Product product = productOptional.get();

            if (product.getQuantity() < itemDTO.getQuantity()) {
                throw new BadRequestAlertException(
                    "Not enough stock for product: " + product.getName() + ". Available: " + product.getQuantity(),
                    "order",
                    "notEnoughStock"
                );
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(itemDTO.getProductId());
            orderItem.setProductName(itemDTO.getProductName());
            orderItem.setQuantity(itemDTO.getQuantity());
            orderItem.setPrice(itemDTO.getPrice());
            orderItem.setOrder(savedOrder);
            orderItems.add(orderItem);

            // Cập nhật số lượng tồn kho sản phẩm
            product.setQuantity(product.getQuantity() - itemDTO.getQuantity());
            productRepository.save(product);
        }
        orderItemRepository.saveAll(orderItems);
        savedOrder.setItems(orderItems);

        return savedOrder;
    }

    @Transactional(readOnly = true)
    public Page<Order> findAll(Pageable pageable) {
        return orderRepository.findAll(pageable);
    }

    public Optional<Order> findOne(Long id) {
        return orderRepository.findById(id);
    }

    public void delete(Long id) {
        log.debug("Request to delete Order : {}", id);
        orderRepository.deleteById(id);
    }

    public List<Order> findOrdersByCurrentUser() {
        Optional<String> userLogin = SecurityUtils.getCurrentUserLogin();
        return userLogin.map(orderRepository::findByCustomer_Login).orElse(List.of());
    }
}
