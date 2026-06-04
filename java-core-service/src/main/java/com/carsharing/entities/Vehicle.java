package com.carsharing.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "vehicles", schema = "main")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false)
    private String model;

    @Column(name = "plate_number", nullable = false, unique = true)
    private String plateNumber;

    @Column(name = "color")
    private String color;

    @Column(nullable = false)
    private String status = "available"; // available, rented, maintenance

    @Column(name = "current_location")
    private String currentLocation;

    @Column(name = "fuel_level")
    private Double fuelLevel;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Vehicle() {}

    public Vehicle(String brand, String model, String plateNumber, String color) {
        this.brand = brand;
        this.model = model;
        this.plateNumber = plateNumber;
        this.color = color;
    }

    // Геттеры и Сеттеры
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getPlateNumber() { return plateNumber; }
    public void setPlateNumber(String plateNumber) { this.plateNumber = plateNumber; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getStatus() { return status; }
    public void setStatus(String status) { 
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }
    public String getCurrentLocation() { return currentLocation; }
    public void setCurrentLocation(String currentLocation) { 
        this.currentLocation = currentLocation;
        this.updatedAt = LocalDateTime.now();
    }
    public Double getFuelLevel() { return fuelLevel; }
    public void setFuelLevel(Double fuelLevel) { 
        this.fuelLevel = fuelLevel;
        this.updatedAt = LocalDateTime.now();
    }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    // Business logic methods
    public boolean isAvailable() {
        return "available".equals(status);
    }
    
    public boolean isRented() {
        return "rented".equals(status);
    }
    
    public void rent() {
        this.status = "rented";
        this.updatedAt = LocalDateTime.now();
    }
    
    public void returnVehicle() {
        this.status = "available";
        this.updatedAt = LocalDateTime.now();
    }
    
    public void putInMaintenance() {
        this.status = "maintenance";
        this.updatedAt = LocalDateTime.now();
    }
}