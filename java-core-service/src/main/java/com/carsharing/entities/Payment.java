package com.carsharing.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments", schema = "main")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "ride_id", nullable = false)
    private UUID rideId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency = "RUB";

    @Column(nullable = false)
    private String status = "pending"; // pending, completed, failed, refunded

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "payment_method")
    private String paymentMethod; // credit_card, debit_card, wallet, etc.

    @Column(name = "transaction_id")
    private String transactionId;

    public Payment() {}

    public Payment(UUID rideId, UUID userId, BigDecimal amount) {
        this.rideId = rideId;
        this.userId = userId;
        this.amount = amount;
    }

    // Геттеры и Сеттеры
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getRideId() { return rideId; }
    public void setRideId(UUID rideId) { this.rideId = rideId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    
    // Business logic methods
    public void completePayment(String paymentMethod, String transactionId) {
        this.status = "completed";
        this.paymentMethod = paymentMethod;
        this.transactionId = transactionId;
        this.completedAt = LocalDateTime.now();
    }
    
    public void failPayment() {
        this.status = "failed";
    }
    
    public void refundPayment() {
        this.status = "refunded";
    }
    
    public boolean isCompleted() {
        return "completed".equals(status);
    }
    
    public boolean isPending() {
        return "pending".equals(status);
    }
}