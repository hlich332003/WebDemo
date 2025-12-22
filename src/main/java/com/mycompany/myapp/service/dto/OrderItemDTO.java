package com.mycompany.myapp.service.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

public class OrderItemDTO implements Serializable {

    private ProductDTO product;
    private Integer quantity;
    private BigDecimal price;
    private String productName;

    public ProductDTO getProduct() {
        return product;
    }

    public void setProduct(ProductDTO product) {
        this.product = product;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
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
            Objects.equals(product, that.product) &&
            Objects.equals(quantity, that.quantity) &&
            Objects.equals(price, that.price) &&
            Objects.equals(productName, that.productName)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(product, quantity, price, productName);
    }

    @Override
    public String toString() {
        return (
            "OrderItemDTO{" +
            "product=" + product +
            ", quantity=" + quantity +
            ", price=" + price +
            ", productName='" + productName +
            "\'}"
        );
    }
}
