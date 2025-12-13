package com.mycompany.myapp.service.dto;

import com.mycompany.myapp.domain.Product;
import java.io.Serializable;
import java.util.Objects;

public class ProductDTO implements Serializable {

    private Long id;
    private String name;
    private String description;
    private Double price;
    private Integer quantity;
    private String imageUrl;
    private Integer salesCount;
    private CategoryDTO category;

    public ProductDTO() {}

    public ProductDTO(Product product) {
        this.id = product.getId();
        this.name = product.getName();
        this.description = product.getDescription();
        this.price = product.getPrice();
        this.quantity = product.getQuantity();
        this.imageUrl = product.getImageUrl();
        this.salesCount = product.getSalesCount();
        if (product.getCategory() != null) {
            this.category = new CategoryDTO(product.getCategory());
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public Integer getSalesCount() {
        return salesCount;
    }

    public void setSalesCount(Integer salesCount) {
        this.salesCount = salesCount;
    }

    public CategoryDTO getCategory() {
        return category;
    }

    public void setCategory(CategoryDTO category) {
        this.category = category;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductDTO that = (ProductDTO) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(name, that.name) &&
            Objects.equals(description, that.description) &&
            Objects.equals(price, that.price) &&
            Objects.equals(quantity, that.quantity) &&
            Objects.equals(imageUrl, that.imageUrl) &&
            Objects.equals(salesCount, that.salesCount) &&
            Objects.equals(category, that.category)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, price, quantity, imageUrl, salesCount, category);
    }

    @Override
    public String toString() {
        return (
            "ProductDTO{" +
            "id=" +
            id +
            ", name='" +
            name +
            "\'" +
            ", description='" +
            description +
            "\'" +
            ", price=" +
            price +
            ", quantity=" +
            quantity +
            ", imageUrl='" +
            imageUrl +
            "\'" +
            ", salesCount=" +
            salesCount +
            ", category=" +
            category +
            '}'
        );
    }
}
