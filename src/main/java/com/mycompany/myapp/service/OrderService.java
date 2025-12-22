package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.*;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.repository.*;
import com.mycompany.myapp.security.SecurityUtils;
import com.mycompany.myapp.service.dto.OrderDTO;
import com.mycompany.myapp.service.dto.OrderEventDTO;
import com.mycompany.myapp.service.dto.OrderItemDTO;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

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
    private final MessageProducer messageProducer;
    private final MailService mailService;
    private final NotificationService notificationService;

    public OrderService(
        OrderRepository orderRepository,
        OrderItemRepository orderItemRepository,
        ProductRepository productRepository,
        UserRepository userRepository,
        CartRepository cartRepository,
        WishlistItemRepository wishlistItemRepository,
        MessageProducer messageProducer,
        MailService mailService,
        NotificationService notificationService
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.cartRepository = cartRepository;
        this.wishlistItemRepository = wishlistItemRepository;
        this.messageProducer = messageProducer;
        this.mailService = mailService;
        this.notificationService = notificationService;
    }

    public Order save(Order order) {
        log.debug("Request to save Order : {}", order);
        return orderRepository.save(order);
    }

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
            orderItem.setPriceAtPurchase(product.getPrice()); // Set price at purchase
            orderItems.add(orderItem);

            totalAmount = totalAmount.add(product.getPrice().multiply(new BigDecimal(itemDTO.getQuantity())));

            product.setQuantity(product.getQuantity() - itemDTO.getQuantity());
            product.setSalesCount(product.getSalesCount() + itemDTO.getQuantity());
            Product updatedProduct = productRepository.save(product);

            // Check for low stock and notify users
            if (updatedProduct.getQuantity() > 0 && updatedProduct.getQuantity() < 5) {
                notifyUsersWithProductInWishlist(updatedProduct);
            }
        }

        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order);

        for (OrderItem item : orderItems) {
            item.setOrder(savedOrder);
        }
        orderItemRepository.saveAll(orderItems);
        savedOrder.setItems(orderItems);

        currentUserOpt.ifPresent(user -> {
            // Notify user of successful order
            notificationService.notifyUserOrderSuccess(user, savedOrder.getOrderCode());
        });

        String customerName = savedOrder.getCustomerFullName() != null ? savedOrder.getCustomerFullName() : "Khách hàng";
        notificationService.notifyAdminNewOrder(savedOrder.getId(), customerName);

        OrderEventDTO orderEvent = new OrderEventDTO(
            savedOrder.getId(),
            savedOrder.getOrderCode(),
            savedOrder.getCustomerEmail(),
            customerName,
            savedOrder.getTotalAmount()
        );
        messageProducer.sendOrderCreatedEvent(orderEvent);
        mailService.sendOrderStatusUpdateEmail(savedOrder, "email.order.confirmation.title", "orderConfirmationEmail");

        return savedOrder;
    }

    private void notifyUsersWithProductInWishlist(Product product) {
        List<WishlistItem> wishlistItems = wishlistItemRepository.findByProduct(product);
        for (WishlistItem item : wishlistItems) {
            User user = item.getUser();
            if (user != null) {
                notificationService.notifyUserLowStock(user, product);
            }
        }
    }

    @CacheEvict(value = "userOrders", allEntries = true)
    public Order updateOrderStatus(Long orderId, OrderStatus newStatus) {
        log.debug("📦 Cập nhật trạng thái đơn hàng {} thành {}", orderId, newStatus);
        Order order = orderRepository
            .findById(orderId)
            .orElseThrow(() -> new BadRequestAlertException("Không tìm thấy đơn hàng", "order", "orderNotFound"));

        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);

        // Gửi email thông báo
        if (newStatus == OrderStatus.PROCESSING) {
            mailService.sendOrderStatusUpdateEmail(updatedOrder, "email.order.confirmed.title", "orderConfirmedEmail");
        } else if (newStatus == OrderStatus.DELIVERED || newStatus == OrderStatus.COMPLETED) {
            mailService.sendOrderStatusUpdateEmail(updatedOrder, "email.order.completed.title", "orderCompletedEmail");
        }

        // Gửi thông báo realtime cho user
        if (updatedOrder.getCustomer() != null) {
            notificationService.notifyOrderStatusChange(
                updatedOrder.getCustomer().getEmail(),
                updatedOrder.getId(),
                newStatus.toString()
            );
        } else if (updatedOrder.getCustomerEmail() != null) {
             userRepository.findOneByEmailIgnoreCase(updatedOrder.getCustomerEmail()).ifPresent(user -> {
                 notificationService.notifyOrderStatusChange(user.getEmail(), updatedOrder.getId(), newStatus.toString());
             });
        }

        return updatedOrder;
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

    public boolean compareOrder(Order o1, Order o2) {
        if (o1 == null || o2 == null) {
            return false;
        }
        return Objects.equals(o1.getId(), o2.getId());
    }

    public List<Order> addOrderToCollectionIfMissing(Collection<Order> orderCollection, Order... ordersToCheck) {
        Set<Long> orderIds = orderCollection.stream().map(Order::getId).collect(Collectors.toSet());
        for (Order order : ordersToCheck) {
            if (order != null && !orderIds.contains(order.getId())) {
                orderCollection.add(order);
            }
        }
        return orderCollection.stream().collect(Collectors.toList());
    }
}
