package com.carsharing.controllers;

import com.carsharing.entities.Vehicle;
import com.carsharing.services.VehicleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {
    private static final Logger logger = LoggerFactory.getLogger(VehicleController.class);

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    /**
     * Add a new vehicle
     * POST /api/vehicles
     */
    @PostMapping
    public ResponseEntity<?> addVehicle(@RequestBody AddVehicleRequest request) {
        logger.info("Adding new vehicle: brand={}, model={}, plateNumber={}", request.brand(), request.model(), request.plateNumber());
        try {
            Vehicle vehicle = vehicleService.addVehicle(
                request.brand(),
                request.model(),
                request.plateNumber(),
                request.color()
            );
            logger.info("Vehicle added successfully: id={}, plateNumber={}", vehicle.getId(), vehicle.getPlateNumber());
            return ResponseEntity.status(HttpStatus.CREATED).body(vehicle);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to add vehicle: plateNumber={}, error={}", request.plateNumber(), e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get vehicle by ID
     * GET /api/vehicles/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getVehicleById(@PathVariable UUID id) {
        logger.info("Getting vehicle by ID: id={}", id);
        try {
            var vehicleOptional = vehicleService.getVehicleById(id);
            if (vehicleOptional.isPresent()) {
                return ResponseEntity.ok(vehicleOptional.get());
            } else {
                logger.warn("Vehicle not found: id={}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Vehicle not found"));
            }
        } catch (Exception e) {
            logger.error("Error getting vehicle: id={}, error={}", id, e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get vehicle by plate number
     * GET /api/vehicles/plate/{plateNumber}
     */
    @GetMapping("/plate/{plateNumber}")
    public ResponseEntity<?> getVehicleByPlateNumber(@PathVariable String plateNumber) {
        logger.info("Getting vehicle by plate number: plateNumber={}", plateNumber);
        try {
            var vehicleOptional = vehicleService.getVehicleByPlateNumber(plateNumber);
            if (vehicleOptional.isPresent()) {
                return ResponseEntity.ok(vehicleOptional.get());
            } else {
                logger.warn("Vehicle not found: plateNumber={}", plateNumber);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Vehicle not found"));
            }
        } catch (Exception e) {
            logger.error("Error getting vehicle: plateNumber={}, error={}", plateNumber, e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get all vehicles
     * GET /api/vehicles
     */
    @GetMapping
    public ResponseEntity<List<Vehicle>> getAllVehicles() {
        logger.info("Getting all vehicles");
        List<Vehicle> vehicles = vehicleService.getAllVehicles();
        logger.info("Retrieved {} vehicles", vehicles.size());
        return ResponseEntity.ok(vehicles);
    }

    /**
     * Get available vehicles
     * GET /api/vehicles/available
     */
    @GetMapping("/available")
    public ResponseEntity<List<Vehicle>> getAvailableVehicles() {
        logger.info("Getting available vehicles");
        List<Vehicle> vehicles = vehicleService.getAvailableVehicles();
        logger.info("Retrieved {} available vehicles", vehicles.size());
        return ResponseEntity.ok(vehicles);
    }

    /**
     * Get rented vehicles
     * GET /api/vehicles/rented
     */
    @GetMapping("/rented")
    public ResponseEntity<List<Vehicle>> getRentedVehicles() {
        logger.info("Getting rented vehicles");
        List<Vehicle> vehicles = vehicleService.getRentedVehicles();
        logger.info("Retrieved {} rented vehicles", vehicles.size());
        return ResponseEntity.ok(vehicles);
    }

    /**
     * Get vehicles in maintenance
     * GET /api/vehicles/maintenance
     */
    @GetMapping("/maintenance")
    public ResponseEntity<List<Vehicle>> getVehiclesInMaintenance() {
        logger.info("Getting vehicles in maintenance");
        List<Vehicle> vehicles = vehicleService.getVehiclesInMaintenance();
        logger.info("Retrieved {} vehicles in maintenance", vehicles.size());
        return ResponseEntity.ok(vehicles);
    }

    /**
     * Update vehicle status
     * PUT /api/vehicles/{id}/status
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateVehicleStatus(
            @PathVariable UUID id,
            @RequestBody UpdateStatusRequest request) {
        logger.info("Updating vehicle status: id={}, status={}", id, request.status());
        try {
            Vehicle vehicle = vehicleService.updateVehicleStatus(id, request.status());
            logger.info("Vehicle status updated: id={}, status={}", id, request.status());
            return ResponseEntity.ok(vehicle);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to update vehicle status: id={}, error={}", id, e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Update vehicle location
     * PUT /api/vehicles/{id}/location
     */
    @PutMapping("/{id}/location")
    public ResponseEntity<?> updateVehicleLocation(
            @PathVariable UUID id,
            @RequestBody UpdateLocationRequest request) {
        logger.info("Updating vehicle location: id={}", id);
        try {
            Vehicle vehicle = vehicleService.updateVehicleLocation(id, request.locationJson());
            logger.info("Vehicle location updated: id={}", id);
            return ResponseEntity.ok(vehicle);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to update vehicle location: id={}, error={}", id, e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Update vehicle fuel level
     * PUT /api/vehicles/{id}/fuel
     */
    @PutMapping("/{id}/fuel")
    public ResponseEntity<?> updateVehicleFuelLevel(
            @PathVariable UUID id,
            @RequestBody UpdateFuelRequest request) {
        logger.info("Updating vehicle fuel level: id={}, fuelLevel={}", id, request.fuelLevel());
        try {
            Vehicle vehicle = vehicleService.updateVehicleFuelLevel(id, request.fuelLevel());
            logger.info("Vehicle fuel level updated: id={}", id);
            return ResponseEntity.ok(vehicle);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to update vehicle fuel level: id={}, error={}", id, e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Put vehicle in maintenance
     * POST /api/vehicles/{id}/maintenance
     */
    @PostMapping("/{id}/maintenance")
    public ResponseEntity<?> putVehicleInMaintenance(@PathVariable UUID id) {
        logger.info("Putting vehicle in maintenance: id={}", id);
        try {
            Vehicle vehicle = vehicleService.putVehicleInMaintenance(id);
            logger.info("Vehicle in maintenance: id={}", id);
            return ResponseEntity.ok(vehicle);
        } catch (IllegalArgumentException e) {
            logger.warn("Vehicle not found for maintenance: id={}, error={}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Return vehicle from maintenance
     * POST /api/vehicles/{id}/return-from-maintenance
     */
    @PostMapping("/{id}/return-from-maintenance")
    public ResponseEntity<?> returnVehicleFromMaintenance(@PathVariable UUID id) {
        logger.info("Returning vehicle from maintenance: id={}", id);
        try {
            Vehicle vehicle = vehicleService.returnVehicleFromMaintenance(id);
            logger.info("Vehicle returned from maintenance: id={}", id);
            return ResponseEntity.ok(vehicle);
        } catch (IllegalArgumentException e) {
            logger.warn("Vehicle not found for return from maintenance: id={}, error={}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(e.getMessage()));
        } catch (IllegalStateException e) {
            logger.warn("Cannot return vehicle from maintenance: id={}, error={}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get vehicle statistics
     * GET /api/vehicles/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getVehicleStatistics() {
        logger.info("Getting vehicle statistics");
        try {
            var statistics = vehicleService.getVehicleStatistics();
            logger.info("Vehicle statistics retrieved");
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            logger.error("Error getting vehicle statistics: error={}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // Request/Response records

    public record AddVehicleRequest(String brand, String model, String plateNumber, String color) {}

    public record UpdateStatusRequest(String status) {}

    public record UpdateLocationRequest(String locationJson) {}

    public record UpdateFuelRequest(Double fuelLevel) {}

    public record ErrorResponse(String message) {}
}