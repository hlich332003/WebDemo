package com.mycompany.myapp.service.dto;

import java.io.Serializable;

/**
 * DTO for ZaloPay payment request
 */
public class ZaloPayOrderRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String appUser;
    private Long amount;
    private String description;
    private String orderCode;

    public ZaloPayOrderRequest() {}

    public ZaloPayOrderRequest(String appUser, Long amount, String description, String orderCode) {
        this.appUser = appUser;
        this.amount = amount;
        this.description = description;
        this.orderCode = orderCode;
    }

    // Getters and Setters
    public String getAppUser() {
        return appUser;
    }

    public void setAppUser(String appUser) {
        this.appUser = appUser;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    @Override
    public String toString() {
        return (
            "ZaloPayOrderRequest{" +
            "appUser='" +
            appUser +
            '\'' +
            ", amount=" +
            amount +
            ", description='" +
            description +
            '\'' +
            ", orderCode='" +
            orderCode +
            '\'' +
            '}'
        );
    }
}
