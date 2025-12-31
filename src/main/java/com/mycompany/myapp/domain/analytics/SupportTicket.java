package com.mycompany.myapp.domain.analytics;

import com.mycompany.myapp.domain.enumeration.TicketStatus;
import com.mycompany.myapp.domain.enumeration.TicketType;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "support_ticket")
public class SupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_email", nullable = false)
    private String userEmail; // Can be email or guest session ID

    @Column(name = "title", length = 500)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private TicketStatus status = TicketStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private TicketType type = TicketType.TICKET;

    @Column(name = "assigned_to")
    private String assignedTo;

    @Column(name = "created_date", nullable = false, columnDefinition = "datetimeoffset")
    private Instant createdDate = Instant.now();

    @Column(name = "last_modified_date", columnDefinition = "datetimeoffset")
    private Instant lastModifiedDate = Instant.now();

    @Column(name = "closed_at", columnDefinition = "datetimeoffset")
    private Instant closedAt;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SupportMessage> messages = new HashSet<>();

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public TicketType getType() {
        return type;
    }

    public void setType(TicketType type) {
        this.type = type;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(Instant lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public Set<SupportMessage> getMessages() {
        return messages;
    }

    public void setMessages(Set<SupportMessage> messages) {
        this.messages = messages;
    }

    public void addMessage(SupportMessage message) {
        messages.add(message);
        message.setTicket(this);
    }

    public void removeMessage(SupportMessage message) {
        messages.remove(message);
        message.setTicket(null);
    }
}
