package com.mycompany.myapp.web.rest.dto;

import java.time.Instant;

public class OrderSearchDTO {

    private Long id;
    private String customerLogin;
    private Instant orderDate;
    private Double totalAmount;
    private String status;

    public OrderSearchDTO(Long id, String customerLogin, Instant orderDate, Double totalAmount, String status) {
        this.id = id;
        this.customerLogin = customerLogin;
        this.orderDate = orderDate;
        this.totalAmount = totalAmount;
        this.status = status;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCustomerLogin() {
        return customerLogin;
    }

    public void setCustomerLogin(String customerLogin) {
        this.customerLogin = customerLogin;
    }

    public Instant getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(Instant orderDate) {
        this.orderDate = orderDate;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
