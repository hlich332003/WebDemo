package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.*;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.repository.*;
import com.mycompany.myapp.security.SecurityUtils;
import com.mycompany.myapp.service.dto.OrderDTO;
import com.mycompany.myapp.service.dto.OrderItemDTO;
import com.mycompany.myapp.service.messaging.OrderMessageProducer;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
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
    private final WishlistItemRepository wishlistItemRepository;
    private final OrderMessageProducer orderMessageProducer;
    private final MailService mailService;
    private final NotificationService notificationService;

    public OrderService(
        OrderRepository orderRepository,
        OrderItemRepository orderItemRepository,
        ProductRepository productRepository,
        UserRepository userRepository,
        CartRepository cartRepository,
        WishlistItemRepository wishlistItemRepository,
        OrderMessageProducer orderMessageProducer,
        MailService mailService,
        NotificationService notificationService
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.cartRepository = cartRepository;
        this.wishlistItemRepository = wishlistItemRepository;
        this.orderMessageProducer = orderMessageProducer;
        this.mailService = mailService;
        this.notificationService = notificationService;
    }

    /**
     * Create new order
     * ✅ OPTIMIZED: Evict dashboard cache when new order is created
     */
    @CacheEvict(value = { "userOrders", "dashboardStats" }, allEntries = true)
    public Order create(OrderDTO orderDTO) {
        Order order = new Order();
        order.setOrderDate(Instant.now());
        order.setStatus(OrderStatus.PENDING);
        order.setOrderCode("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setNotes(orderDTO.getNotes());

        Optional<User> currentUserOpt = SecurityUtils.getCurrentUserLogin().flatMap(userRepository::findOneWithAuthoritiesByEmailOrPhone);
        currentUserOpt.ifPresent(order::setCustomer);

        if (orderDTO.getCustomerInfo() != null) {
            order.setCustomerFullName(orderDTO.getCustomerInfo().getFullName());
            order.setCustomerEmail(orderDTO.getCustomerInfo().getEmail());
            order.setCustomerPhone(orderDTO.getCustomerInfo().getPhone());
            order.setDeliveryAddress(orderDTO.getCustomerInfo().getAddress());
            order.setPaymentMethod(orderDTO.getCustomerInfo().getPaymentMethod());
        }

        Set<OrderItem> orderItems = new HashSet<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItemDTO itemDTO : orderDTO.getItems()) {
            if (itemDTO.getProduct() == null || itemDTO.getProduct().getId() == null) {
                throw new BadRequestAlertException("ID sản phẩm không được để trống", "order", "productNotFound");
            }
            Product product = productRepository
                .findById(itemDTO.getProduct().getId())
                .orElseThrow(() -> new BadRequestAlertException("Không tìm thấy sản phẩm", "order", "productNotFound"));

            if (product.getQuantity() < itemDTO.getQuantity()) {
                throw new BadRequestAlertException(
                    "Không đủ hàng cho sản phẩm: " + product.getName() + ". Hiện có: " + product.getQuantity(),
                    "order",
                    "notEnoughStock"
                );
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setProductName(product.getName());
            orderItem.setQuantity(itemDTO.getQuantity());
            orderItem.setPrice(product.getPrice());
            orderItem.setPriceAtPurchase(product.getPrice());
            orderItems.add(orderItem);

            totalAmount = totalAmount.add(product.getPrice().multiply(new BigDecimal(itemDTO.getQuantity())));

            product.setQuantity(product.getQuantity() - itemDTO.getQuantity());
            product.setSalesCount(product.getSalesCount() + itemDTO.getQuantity());
            productRepository.save(product);
        }

        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order);

        for (OrderItem item : orderItems) {
            item.setOrder(savedOrder);
        }
        orderItemRepository.saveAll(orderItems);
        savedOrder.setItems(orderItems);

        // --- NOTIFICATION LOGIC (CORRECTED) ---
        currentUserOpt.ifPresent(user -> {
            notificationService.notifyUserOrderSuccess(user.getId(), savedOrder.getId(), savedOrder.getOrderCode());
        });
        notificationService.notifyAdminNewOrder(savedOrder.getId(), savedOrder.getOrderCode());
        // --- END NOTIFICATION LOGIC ---

        // Send order to RabbitMQ for async processing
        orderMessageProducer.publishOrderCreated(savedOrder);
        // mailService.sendOrderStatusUpdateEmail(savedOrder, "email.order.confirmation.title", "orderConfirmationEmail");

        return savedOrder;
    }

    @CacheEvict(value = "userOrders", allEntries = true)
    public Order updateOrderStatus(Long orderId, OrderStatus newStatus) {
        log.debug("📦 Cập nhật trạng thái đơn hàng {} thành {}", orderId, newStatus);
        Order order = orderRepository
            .findById(orderId)
            .orElseThrow(() -> new BadRequestAlertException("Không tìm thấy đơn hàng", "order", "orderNotFound"));

        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);

        // Gửi email và notification theo từng trạng thái
        String customerEmail = updatedOrder.getCustomerEmail();
        String orderCode = updatedOrder.getOrderCode();

        switch (newStatus) {
            case PROCESSING:
                mailService.sendOrderStatusUpdateEmail(updatedOrder, "email.order.confirmed.title", "orderConfirmedEmail");
                if (customerEmail != null) {
                    notificationService.notifyUserOrderConfirmed(customerEmail, orderId, orderCode);
                }
                break;
            case SHIPPED:
                if (customerEmail != null) {
                    notificationService.notifyUserOrderShipped(customerEmail, orderId, orderCode);
                }
                break;
            case DELIVERED:
                mailService.sendOrderStatusUpdateEmail(updatedOrder, "email.order.completed.title", "orderCompletedEmail");
                if (customerEmail != null) {
                    notificationService.notifyUserOrderDelivered(customerEmail, orderId, orderCode);
                }
                break;
            case COMPLETED:
                mailService.sendOrderStatusUpdateEmail(updatedOrder, "email.order.completed.title", "orderCompletedEmail");
                if (customerEmail != null) {
                    notificationService.notifyUserOrderDelivered(customerEmail, orderId, orderCode);
                }
                break;
            case CANCELLED:
                String customerName = updatedOrder.getCustomerFullName() != null ? updatedOrder.getCustomerFullName() : "Khách hàng";
                String reason = updatedOrder.getNotes() != null ? updatedOrder.getNotes() : "Không có lý do";
                notificationService.notifyAdminOrderCancelled(orderId, customerName, reason, orderCode);
                break;
            default:
                // Các trạng thái khác gửi notification chung
                if (customerEmail != null) {
                    notificationService.notifyOrderStatusChange(customerEmail, orderId, newStatus.toString(), orderCode);
                }
                break;
        }

        return updatedOrder;
    }

    // Other methods...
    public Order save(Order order) {
        log.debug("Request to save Order : {}", order);
        return orderRepository.save(order);
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
