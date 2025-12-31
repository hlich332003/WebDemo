package com.mycompany.myapp.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.Instant;

/**
 * Entity lưu thông tin khách hàng mua offline (chưa có tài khoản)
 */
@Entity
@Table(name = "customer")
public class Customer implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(max = 10)
    @Column(name = "phone", length = 10, unique = true, nullable = false)
    private String phone;

    @Size(max = 100)
    @Column(name = "full_name", length = 100, columnDefinition = "NVARCHAR(100)")
    private String fullName;

    @Column(name = "products_purchased", columnDefinition = "NVARCHAR(MAX)")
    private String productsPurchased; // Danh sách sản phẩm đã mua (phân cách bởi dấu phẩy)

    @Column(name = "created_date", nullable = false)
    private Instant createdDate;

    @Column(name = "last_purchase_date")
    private Instant lastPurchaseDate;

    @Column(name = "notes", columnDefinition = "NVARCHAR(MAX)")
    private String notes;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user; // Liên kết với tài khoản (nếu khách đăng ký sau)

    // Constructors
    public Customer() {
        this.createdDate = Instant.now();
        this.lastPurchaseDate = Instant.now();
    }

    public Customer(String phone, String fullName, String productsPurchased) {
        this();
        this.phone = phone;
        this.fullName = fullName;
        this.productsPurchased = productsPurchased;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getProductsPurchased() {
        return productsPurchased;
    }

    public void setProductsPurchased(String productsPurchased) {
        this.productsPurchased = productsPurchased;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public Instant getLastPurchaseDate() {
        return lastPurchaseDate;
    }

    public void setLastPurchaseDate(Instant lastPurchaseDate) {
        this.lastPurchaseDate = lastPurchaseDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return (
            "Customer{" +
            "id=" +
            id +
            ", phone='" +
            phone +
            '\'' +
            ", fullName='" +
            fullName +
            '\'' +
            ", productsPurchased='" +
            productsPurchased +
            '\'' +
            ", lastPurchaseDate=" +
            lastPurchaseDate +
            '}'
        );
    }
}
