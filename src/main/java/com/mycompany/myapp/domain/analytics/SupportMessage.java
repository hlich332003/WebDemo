package com.mycompany.myapp.domain.analytics;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "support_message")
public class SupportMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private SupportTicket ticket;

    @Column(name = "sender_email", nullable = false)
    private String senderEmail;

    @Lob
    @Column(name = "message", nullable = false)
    private String message;

    @Column(name = "is_from_admin", nullable = false)
    private Boolean isFromAdmin = false;

    @Column(name = "created_at", nullable = false, columnDefinition = "datetimeoffset")
    private Instant createdAt = Instant.now();

    @Column(name = "is_read")
    private Boolean isRead = false;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SupportTicket getTicket() {
        return ticket;
    }

    public void setTicket(SupportTicket ticket) {
        this.ticket = ticket;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getIsFromAdmin() {
        return isFromAdmin;
    }

    public void setIsFromAdmin(Boolean fromAdmin) {
        isFromAdmin = fromAdmin;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean read) {
        isRead = read;
    }
}
