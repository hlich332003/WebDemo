package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.Cart;
import com.mycompany.myapp.domain.Order;
import com.mycompany.myapp.domain.OrderItem;
import com.mycompany.myapp.domain.Product;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.repository.CartRepository;
import com.mycompany.myapp.repository.OrderItemRepository;
import com.mycompany.myapp.repository.OrderRepository;
import com.mycompany.myapp.repository.ProductRepository;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.security.SecurityUtils;
import com.mycompany.myapp.service.dto.OrderDTO;
import com.mycompany.myapp.service.dto.OrderEventDTO;
import com.mycompany.myapp.service.dto.OrderItemDTO;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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
    private final CartRepository cartRepository;
    private final MessageProducer messageProducer;

    public OrderService(
        OrderRepository orderRepository,
        OrderItemRepository orderItemRepository,
        ProductRepository productRepository,
        UserRepository userRepository,
        CartRepository cartRepository,
        MessageProducer messageProducer
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.cartRepository = cartRepository;
        this.messageProducer = messageProducer;
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
        order.setStatus(OrderStatus.PENDING);
        order.setOrderCode("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setNotes(orderDTO.getNotes());

        Optional<User> currentUser = SecurityUtils.getCurrentUserLogin().flatMap(userRepository::findOneWithAuthoritiesByEmailOrPhone);
        currentUser.ifPresent(order::setCustomer);

        if (orderDTO.getCustomerInfo() != null) {
            order.setCustomerFullName(orderDTO.getCustomerInfo().getFullName());
            order.setCustomerEmail(orderDTO.getCustomerInfo().getEmail());
            order.setCustomerPhone(orderDTO.getCustomerInfo().getPhone());
            order.setDeliveryAddress(orderDTO.getCustomerInfo().getAddress());
            order.setPaymentMethod(orderDTO.getCustomerInfo().getPaymentMethod());
        }

        Order savedOrder = orderRepository.save(order);

        Set<OrderItem> orderItems = new HashSet<>();
        for (OrderItemDTO itemDTO : orderDTO.getItems()) {
            Product product = productRepository
                .findById(itemDTO.getProductId())
                .orElseThrow(() -> new BadRequestAlertException("Product not found", "order", "productNotFound"));

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

            product.setQuantity(product.getQuantity() - itemDTO.getQuantity());
            product.setSalesCount(product.getSalesCount() + itemDTO.getQuantity());
            productRepository.save(product);
        }
        orderItemRepository.saveAll(orderItems);
        savedOrder.setItems(orderItems);

        // Clear the user's cart
        currentUser.ifPresent(user -> {
            cartRepository
                .findOneByUser_Email(user.getEmail())
                .ifPresent(cart -> {
                    log.debug("Deleting cart {} for user {}", cart.getId(), user.getEmail());
                    cartRepository.delete(cart);
                });
        });

        String customerName = savedOrder.getCustomerFullName() != null ? savedOrder.getCustomerFullName() : "Khách hàng";

        OrderEventDTO orderEvent = new OrderEventDTO(
            savedOrder.getId(),
            savedOrder.getOrderCode(),
            savedOrder.getCustomerEmail(),
            customerName,
            savedOrder.getTotalAmount()
        );
        messageProducer.sendOrderCreatedEvent(orderEvent);

        return savedOrder;
    }

    @Transactional(readOnly = true)
    public Page<Order> findAll(Pageable pageable) {
        return orderRepository.findAllOrders(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Order> findByOrderCodeContaining(String orderCode, Pageable pageable) {
        log.debug("Request to find Orders by orderCode containing: {}", orderCode);
        return orderRepository.findByOrderCodeContainingIgnoreCase(orderCode, pageable);
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
        return userLogin.map(orderRepository::findByCustomer_EmailOrderByOrderDateDesc).orElse(List.of());
    }
}
