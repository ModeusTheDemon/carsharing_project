package com.carsharing.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "rides", schema = "main")
public class Ride {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "start_location", columnDefinition = "TEXT")
    private String startLocation;

    @Column(name = "end_location", columnDefinition = "TEXT")
    private String endLocation;

    @Column(name = "total_cost", precision = 10, scale = 2)
    private BigDecimal totalCost;

    @Column(nullable = false)
    private String status = "active"; // active, completed, cancelled

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Ride() {}

    public Ride(UUID userId, UUID vehicleId, String startLocation) {
        this.userId = userId;
        this.vehicleId = vehicleId;
        this.startLocation = startLocation;
        this.startTime = LocalDateTime.now();
        this.status = "active";
    }

    // Геттеры и Сеттеры
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getVehicleId() { return vehicleId; }
    public void setVehicleId(UUID vehicleId) { this.vehicleId = vehicleId; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public String getStartLocation() { return startLocation; }
    public void setStartLocation(String startLocation) { this.startLocation = startLocation; }
    public String getEndLocation() { return endLocation; }
    public void setEndLocation(String endLocation) { this.endLocation = endLocation; }
    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    // Business logic methods
    public void completeRide(String endLocation, BigDecimal totalCost) {
        this.endLocation = endLocation;
        this.totalCost = totalCost;
        this.endTime = LocalDateTime.now();
        this.status = "completed";
    }
    
    public void cancelRide() {
        this.endTime = LocalDateTime.now();
        this.status = "cancelled";
    }
    
    public boolean isActive() {
        return "active".equals(status);
    }
    
    public boolean isCompleted() {
        return "completed".equals(status);
    }
}