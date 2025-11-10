package com.mycompany.myapp.service.dto;

import java.io.Serializable;
import java.util.Objects;

public class OrderItemDTO implements Serializable {

    private Long productId;
    private Integer quantity;
    private Double price;
    private String productName; // Thêm productName để lưu vào OrderItem entity

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderItemDTO that = (OrderItemDTO) o;
        return (
            Objects.equals(productId, that.productId) &&
            Objects.equals(quantity, that.quantity) &&
            Objects.equals(price, that.price) &&
            Objects.equals(productName, that.productName)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, quantity, price, productName);
    }

    @Override
    public String toString() {
        return (
            "OrderItemDTO{" +
            "productId=" +
            productId +
            ", quantity=" +
            quantity +
            ", price=" +
            price +
            ", productName='" +
            productName +
            "\'}"
        );
    }
}
