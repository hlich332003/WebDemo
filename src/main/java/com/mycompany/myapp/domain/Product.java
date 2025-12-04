package com.mycompany.myapp.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mycompany.myapp.domain.AbstractAuditingEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;

@Entity
@Table(
    name = "jhi_product",
    indexes = {
        @Index(name = "idx_product_category", columnList = "category_id"),
        @Index(name = "idx_product_name", columnList = "name"),
        @Index(name = "idx_product_sales", columnList = "sales_count")
    }
)
public class Product extends AbstractAuditingEntity<Long> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Size(min = 3, max = 100)
    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Size(max = 500)
    @Column(name = "description", length = 500, columnDefinition = "NVARCHAR(500)")
    private String description;

    @NotNull
    @DecimalMin(value = "0.0")
    @Column(name = "price", nullable = false)
    private Double price;

    @NotNull
    @Min(value = 0L)
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "image_url", columnDefinition = "NVARCHAR(MAX)") // Cập nhật để khớp với DB
    private String imageUrl;

    // Active flag - mapped to DB column `is_active` which is NOT NULL in schema.
    // Add as Boolean with default TRUE so JPA includes it on INSERTs and avoids NULL constraint failures.
    @NotNull
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = Boolean.TRUE;

    // Sales count from DB (used to surface best-sellers)
    @Column(name = "sales_count", nullable = false)
    private Long salesCount = 0L;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    @JsonIgnoreProperties(value = { "products", "hibernateLazyInitializer", "handler" }, allowSetters = true)
    private Category category;

    // Getters and setters

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

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Long getSalesCount() {
        return salesCount;
    }

    public void setSalesCount(Long salesCount) {
        this.salesCount = salesCount;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Product)) {
            return false;
        }
        return id != null && id.equals(((Product) o).id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return (
            "Product{" +
            "id=" +
            getId() +
            ", name='" +
            getName() +
            "'" +
            ", description='" +
            getDescription() +
            "'" +
            ", price=" +
            getPrice() +
            ", quantity=" +
            getQuantity() +
            ", imageUrl='" +
            getImageUrl() +
            "'" +
            ", isActive=" +
            isActive +
            ", salesCount=" +
            salesCount +
            "}"
        );
    }
}
