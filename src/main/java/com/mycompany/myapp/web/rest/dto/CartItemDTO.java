package com.mycompany.myapp.web.rest.dto;

import com.mycompany.myapp.domain.CartItem;

public class CartItemDTO {

    private Long productId;
    private String productName;
    private Double price;
    private Integer quantity;
    private String imageUrl;

    public CartItemDTO() {
        // Empty constructor needed for Jackson.
    }

    public CartItemDTO(CartItem cartItem) {
        this.productId = cartItem.getProduct().getId();
        this.productName = cartItem.getProduct().getName();
        this.price = cartItem.getProduct().getPrice();
        this.quantity = cartItem.getQuantity();
        this.imageUrl = cartItem.getProduct().getImageUrl();
    }

    // Getters and Setters
    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
