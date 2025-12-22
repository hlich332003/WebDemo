package com.mycompany.myapp.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.web.rest.dto.OrderSearchDTO;
import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "jhi_order")
@SqlResultSetMapping(
    name = "OrderSearchDTOMapping",
    classes = @ConstructorResult(
        targetClass = OrderSearchDTO.class,
        columns = {
            @ColumnResult(name = "id", type = Long.class),
            @ColumnResult(name = "customerLogin", type = String.class),
            @ColumnResult(name = "orderDate", type = Instant.class),
            @ColumnResult(name = "totalAmount", type = BigDecimal.class),
            @ColumnResult(name = "status", type = String.class),
        }
    )
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Order implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "order_date")
    private Instant orderDate;

    @Column(name = "total_amount", precision = 18, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50, columnDefinition = "NVARCHAR(50)")
    private OrderStatus status;

    @Column(name = "customer_full_name", length = 255, columnDefinition = "NVARCHAR(255)")
    private String customerFullName;

    @Column(name = "customer_email", length = 254, columnDefinition = "NVARCHAR(254)")
    private String customerEmail;

    @Column(name = "customer_phone", length = 10, columnDefinition = "NVARCHAR(10)")
    private String customerPhone;

    @Column(name = "delivery_address", length = 255, columnDefinition = "NVARCHAR(255)")
    private String deliveryAddress;

    @Column(name = "payment_method", length = 50, columnDefinition = "NVARCHAR(50)")
    private String paymentMethod;

    @Column(name = "order_code", unique = true, nullable = false, length = 255, columnDefinition = "NVARCHAR(255)")
    private String orderCode;

    @Column(name = "notes", length = 500, columnDefinition = "NVARCHAR(500)")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "user_id")
    @JsonIgnoreProperties(value = { "authorities", "orders" }, allowSetters = true)
    private User customer;

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties(value = { "order" }, allowSetters = true)
    private Set<OrderItem> items = new HashSet<>();

    // Getters and setters

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Instant getOrderDate() {
        return this.orderDate;
    }

    public void setOrderDate(Instant orderDate) {
        this.orderDate = orderDate;
    }

    public BigDecimal getTotalAmount() {
        return this.totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public OrderStatus getStatus() {
        return this.status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public String getCustomerFullName() {
        return this.customerFullName;
    }

    public void setCustomerFullName(String customerFullName) {
        this.customerFullName = customerFullName;
    }

    public String getCustomerEmail() {
        return this.customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerPhone() {
        return this.customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getDeliveryAddress() {
        return this.deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public String getPaymentMethod() {
        return this.paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getOrderCode() {
        return this.orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public String getNotes() {
        return this.notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public User getCustomer() {
        return this.customer;
    }

    public void setCustomer(User user) {
        this.customer = user;
    }

    public Set<OrderItem> getItems() {
        return this.items;
    }

    public void setItems(Set<OrderItem> orderItems) {
        if (this.items != null) {
            this.items.forEach(e -> e.setOrder(null));
        }
        if (orderItems != null) {
            orderItems.forEach(e -> e.setOrder(this));
        }
        this.items = orderItems;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Order)) {
            return false;
        }
        Order order = (Order) o;
        if (this.id == null) {
            return false;
        }
        return getClass().hashCode() == order.getClass().hashCode() && id.equals(order.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Order{" +
            "id=" + getId() +
            ", orderDate='" + getOrderDate() + "'" +
            ", totalAmount=" + getTotalAmount() +
            ", status='" + getStatus() + "'" +
            ", customerFullName='" + getCustomerFullName() + "'" +
            ", customerEmail='" + getCustomerEmail() + "'" +
            ", customerPhone='" + getCustomerPhone() + "'" +
            ", deliveryAddress='" + getDeliveryAddress() + "'" +
            ", paymentMethod='" + getPaymentMethod() + "'" +
            "}";
    }
}
