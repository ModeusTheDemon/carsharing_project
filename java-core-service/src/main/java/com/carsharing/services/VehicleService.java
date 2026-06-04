package com.carsharing.services;

import com.carsharing.entities.Vehicle;
import com.carsharing.repositories.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    @Transactional
    public Vehicle addVehicle(String brand, String model, String plateNumber, String color) {
        if (vehicleRepository.findByPlateNumber(plateNumber) != null) {
            throw new IllegalArgumentException("Vehicle with this plate number already exists");
        }
        
        if (brand == null || brand.trim().isEmpty()) {
            throw new IllegalArgumentException("Brand is required");
        }
        if (model == null || model.trim().isEmpty()) {
            throw new IllegalArgumentException("Model is required");
        }
        if (plateNumber == null || plateNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Plate number is required");
        }
        
        Vehicle vehicle = new Vehicle(brand, model, plateNumber, color);
        return vehicleRepository.save(vehicle);
    }

    public Optional<Vehicle> getVehicleById(UUID vehicleId) {
        return vehicleRepository.findById(vehicleId);
    }

    public Optional<Vehicle> getVehicleByPlateNumber(String plateNumber) {
        Vehicle vehicle = vehicleRepository.findByPlateNumber(plateNumber);
        return Optional.ofNullable(vehicle);
    }

    public List<Vehicle> getAllVehicles() {
        return vehicleRepository.findAll();
    }

    public List<Vehicle> getAvailableVehicles() {
        return vehicleRepository.findByStatus("available");
    }

    public List<Vehicle> getRentedVehicles() {
        return vehicleRepository.findByStatus("rented");
    }

    public List<Vehicle> getVehiclesInMaintenance() {
        return vehicleRepository.findByStatus("maintenance");
    }

    @Transactional
    public Vehicle updateVehicleStatus(UUID vehicleId, String status) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));
        
        if (!isValidStatus(status)) {
            throw new IllegalArgumentException("Invalid vehicle status: " + status);
        }
        
        vehicle.setStatus(status);
        return vehicleRepository.save(vehicle);
    }

    @Transactional
    public Vehicle updateVehicleLocation(UUID vehicleId, String locationJson) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));
        
        vehicle.setCurrentLocation(locationJson);
        return vehicleRepository.save(vehicle);
    }

    @Transactional
    public Vehicle updateVehicleFuelLevel(UUID vehicleId, Double fuelLevel) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));
        
        if (fuelLevel < 0 || fuelLevel > 100) {
            throw new IllegalArgumentException("Fuel level must be between 0 and 100");
        }
        
        vehicle.setFuelLevel(fuelLevel);
        return vehicleRepository.save(vehicle);
    }

    @Transactional
    public Vehicle putVehicleInMaintenance(UUID vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));
        
        vehicle.putInMaintenance();
        return vehicleRepository.save(vehicle);
    }

    @Transactional
    public Vehicle returnVehicleFromMaintenance(UUID vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));
        
        if (!"maintenance".equals(vehicle.getStatus())) {
            throw new IllegalStateException("Vehicle is not in maintenance");
        }
        
        vehicle.returnVehicle();
        return vehicleRepository.save(vehicle);
    }

    public boolean isVehicleAvailable(UUID vehicleId) {
        return vehicleRepository.findById(vehicleId)
                .map(Vehicle::isAvailable)
                .orElse(false);
    }

    public VehicleStatistics getVehicleStatistics() {
        List<Vehicle> allVehicles = vehicleRepository.findAll();
        
        long totalVehicles = allVehicles.size();
        long availableVehicles = allVehicles.stream().filter(Vehicle::isAvailable).count();
        long rentedVehicles = allVehicles.stream().filter(Vehicle::isRented).count();
        long maintenanceVehicles = allVehicles.stream().filter(v -> "maintenance".equals(v.getStatus())).count();
        
        Double averageFuelLevel = allVehicles.stream()
                .filter(v -> v.getFuelLevel() != null)
                .mapToDouble(Vehicle::getFuelLevel)
                .average()
                .orElse(0.0);
        
        return new VehicleStatistics(
                totalVehicles, availableVehicles, rentedVehicles, 
                maintenanceVehicles, averageFuelLevel
        );
    }

    private boolean isValidStatus(String status) {
        return List.of("available", "rented", "maintenance").contains(status);
    }

    public record VehicleStatistics(
            long totalVehicles,
            long availableVehicles,
            long rentedVehicles,
            long maintenanceVehicles,
            Double averageFuelLevel
    ) {}
}