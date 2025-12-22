package com.mycompany.myapp.domain.analytics;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.Instant;

/**
 * Entity Notification - Lưu trong analytics_db
 * Quản lý thông báo real-time cho người dùng
 */
@Entity
@Table(name = "notification")
public class Notification implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Size(max = 500)
    @Column(name = "message", length = 500, nullable = false)
    private String message;

    @NotNull
    @Size(max = 20)
    @Column(name = "type", length = 20, nullable = false)
    private String type;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @NotNull
    @Size(max = 255)
    @Column(name = "user_id", length = 255, nullable = false)
    private String userId;

    @NotNull
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Size(max = 500)
    @Column(name = "link", length = 500)
    private String link;

    @Size(max = 50)
    @Column(name = "target_role", length = 50)
    private String targetRole;

    // Constructors
    public Notification() {
        this.createdAt = Instant.now();
        this.isRead = false;
    }

    public Notification(String message, String type, String userId) {
        this.message = message;
        this.type = type;
        this.userId = userId;
        this.createdAt = Instant.now();
        this.isRead = false;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Notification)) {
            return false;
        }
        return id != null && id.equals(((Notification) o).id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Notification{" +
            "id=" + getId() +
            ", message='" + getMessage() + "'" +
            ", type='" + getType() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            ", userId='" + getUserId() + "'" +
            ", isRead='" + getIsRead() + "'" +
            ", link='" + getLink() + "'" +
            ", targetRole='" + getTargetRole() + "'" +
            "}";
    }
}
