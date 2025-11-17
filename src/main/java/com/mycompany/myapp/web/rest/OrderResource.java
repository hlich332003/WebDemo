package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.domain.Order;
import com.mycompany.myapp.domain.enumeration.OrderStatus; // Import OrderStatus
import com.mycompany.myapp.repository.OrderRepository;
import com.mycompany.myapp.security.AuthoritiesConstants;
import com.mycompany.myapp.security.SecurityUtils;
import com.mycompany.myapp.service.OrderService;
import com.mycompany.myapp.service.dto.OrderDTO;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link com.mycompany.myapp.domain.Order}.
 */
@RestController
@RequestMapping(value = "/api", produces = "application/json; charset=UTF-8")
public class OrderResource {

    private final Logger log = LoggerFactory.getLogger(OrderResource.class);

    private static final String ENTITY_NAME = "order";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final OrderService orderService;
    private final OrderRepository orderRepository;

    // private final com.mycompany.myapp.service.NotificationService notificationService; // Removed

    public OrderResource(
        OrderService orderService,
        OrderRepository orderRepository/*, com.mycompany.myapp.service.NotificationService notificationService*/
    ) { // Removed parameter
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        // this.notificationService = notificationService; // Removed
    }

    @PostMapping(value = "/orders", consumes = "application/json; charset=UTF-8", produces = "application/json; charset=UTF-8")
    public ResponseEntity<Order> createOrder(@RequestBody OrderDTO orderDTO) throws URISyntaxException {
        log.info("REST request to save Order : {}", orderDTO);
        log.info("CustomerInfo: {}", orderDTO.getCustomerInfo());
        log.info("Items: {}", orderDTO.getItems());
        log.info("TotalAmount: {}", orderDTO.getTotalAmount());

        if (orderDTO.getCustomerInfo() == null || orderDTO.getItems() == null || orderDTO.getItems().isEmpty()) {
            log.error("Invalid order data - customerInfo: {}, items: {}", orderDTO.getCustomerInfo(), orderDTO.getItems());
            throw new BadRequestAlertException("Invalid order data", ENTITY_NAME, "orderInvalidData");
        }

        Order result = orderService.create(orderDTO);

        // Gửi thông báo WebSocket cho admin // Removed
        // String customerName = orderDTO.getCustomerInfo() != null ? orderDTO.getCustomerInfo().getFullName() : "Khách hàng";
        // notificationService.notifyNewOrder(result.getId(), customerName);

        return ResponseEntity.created(new URI("/api/orders/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    @PatchMapping("/orders/{id}/status")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<Order> updateOrderStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        log.debug("REST request to update status for Order : {} to {}", id, payload.get("status"));
        Optional<Order> existingOrder = orderService.findOne(id);
        if (existingOrder.isEmpty()) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }
        Order order = existingOrder.get();
        String statusString = payload.get("status");
        if (statusString == null || statusString.isEmpty()) {
            throw new BadRequestAlertException("Status cannot be empty", ENTITY_NAME, "statusEmpty");
        }
        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(statusString);
        } catch (IllegalArgumentException e) {
            throw new BadRequestAlertException("Invalid status value: " + statusString, ENTITY_NAME, "invalidStatus");
        }
        order.setStatus(newStatus);
        Order result = orderService.save(order);

        // Gửi thông báo WebSocket // Removed
        // notificationService.notifyOrderStatusChange(result.getId(), newStatus.name());

        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    @PatchMapping("/orders/{id}/cancel")
    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
    public ResponseEntity<Order> cancelOrder(@PathVariable Long id) {
        log.debug("REST request to cancel Order : {}", id);
        Optional<Order> existingOrder = orderService.findOne(id);
        if (existingOrder.isEmpty()) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }
        Order order = existingOrder.get();
        if (
            SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.USER) &&
            (order.getCustomer() == null || !order.getCustomer().getLogin().equals(SecurityUtils.getCurrentUserLogin().orElse(null)))
        ) {
            throw new BadRequestAlertException("You are not authorized to cancel this order", ENTITY_NAME, "unauthorized");
        }
        order.setStatus(OrderStatus.CANCELLED); // Sử dụng Enum
        Order result = orderService.save(order);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    @PatchMapping(
        value = "/orders/{id}/address",
        consumes = "application/json; charset=UTF-8",
        produces = "application/json; charset=UTF-8"
    )
    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
    public ResponseEntity<Order> updateOrderAddress(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        log.debug("REST request to update address for Order : {} to {}", id, payload.get("address"));
        Optional<Order> existingOrder = orderService.findOne(id);
        if (existingOrder.isEmpty()) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }
        Order order = existingOrder.get();
        String address = payload.get("address");
        if (address == null || address.isEmpty()) {
            throw new BadRequestAlertException("Address cannot be empty", ENTITY_NAME, "addressEmpty");
        }
        if (
            SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.USER) &&
            (order.getCustomer() == null || !order.getCustomer().getLogin().equals(SecurityUtils.getCurrentUserLogin().orElse(null)))
        ) {
            throw new BadRequestAlertException("You are not authorized to update this order's address", ENTITY_NAME, "unauthorized");
        }
        order.setDeliveryAddress(address);
        Order result = orderService.save(order);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    @GetMapping("/orders")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<List<Order>> getAllOrders(@org.springdoc.core.annotations.ParameterObject Pageable pageable) {
        log.debug("REST request to get a page of Orders");
        Page<Order> page = orderService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/orders/{id}")
    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
    public ResponseEntity<Order> getOrder(@PathVariable Long id) {
        log.debug("REST request to get Order : {}", id);
        Optional<Order> order = orderService.findOne(id);
        if (order.isEmpty()) {
            return ResponseUtil.wrapOrNotFound(order);
        }
        if (
            SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.USER) &&
            (order.get().getCustomer() == null ||
                !order.get().getCustomer().getLogin().equals(SecurityUtils.getCurrentUserLogin().orElse(null)))
        ) {
            throw new BadRequestAlertException("You are not authorized to view this order", ENTITY_NAME, "unauthorized");
        }
        return ResponseUtil.wrapOrNotFound(order);
    }

    @GetMapping("/my-orders")
    public ResponseEntity<List<Order>> getMyOrders() {
        log.debug("REST request to get current user's Orders");
        List<Order> orders = orderService.findOrdersByCurrentUser();
        return ResponseEntity.ok().body(orders);
    }

    @DeleteMapping("/orders/{id}")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        log.debug("REST request to delete Order : {}", id);
        orderService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
