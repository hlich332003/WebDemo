package com.mycompany.myapp.service.dto;

import java.io.Serializable;

public class OrderEventDTO implements Serializable {

    private Long orderId;
    private String orderCode;
    private String customerEmail;
    private String customerName;
    private Double totalAmount;

    public OrderEventDTO() {}

    public OrderEventDTO(Long orderId, String orderCode, String customerEmail, String customerName, Double totalAmount) {
        this.orderId = orderId;
        this.orderCode = orderCode;
        this.customerEmail = customerEmail;
        this.customerName = customerName;
        this.totalAmount = totalAmount;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }
}
