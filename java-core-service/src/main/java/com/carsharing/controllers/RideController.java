package com.carsharing.controllers;

import com.carsharing.entities.Ride;
import com.carsharing.services.RideService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/rides")
public class RideController {
    private static final Logger logger = LoggerFactory.getLogger(RideController.class);

    private final RideService rideService;

    public RideController(RideService rideService) {
        this.rideService = rideService;
    }

    /**
     * Start a new ride
     * POST /api/rides/start
     * Validates: Requirements 2.1
     */
    @PostMapping("/start")
    public ResponseEntity<?> startRide(@RequestBody StartRideRequest request) {
        logger.info("Starting ride: userId={}, vehicleId={}", request.userId(), request.vehicleId());
        try {
            Ride ride = rideService.startRide(
                request.userId(),
                request.vehicleId(),
                request.startLocation()
            );
            logger.info("Ride started successfully: id={}, userId={}", ride.getId(), request.userId());
            return ResponseEntity.status(HttpStatus.CREATED).body(ride);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to start ride: userId={}, vehicleId={}, error={}", request.userId(), request.vehicleId(), e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (IllegalStateException e) {
            logger.warn("Cannot start ride: userId={}, vehicleId={}, error={}", request.userId(), request.vehicleId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * End a ride
     * POST /api/rides/{id}/end
     * Validates: Requirements 2.2
     */
    @PostMapping("/{id}/end")
    public ResponseEntity<?> endRide(
            @PathVariable UUID id,
            @RequestBody EndRideRequest request) {
        logger.info("Ending ride: id={}", id);
        try {
            Ride ride = rideService.endRide(id, request.endLocation());
            logger.info("Ride ended successfully: id={}", id);
            return ResponseEntity.ok(ride);
        } catch (IllegalArgumentException e) {
            logger.warn("Ride not found for end: id={}, error={}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(e.getMessage()));
        } catch (IllegalStateException e) {
            logger.warn("Cannot end ride: id={}, error={}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Cancel a ride
     * POST /api/rides/{id}/cancel
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelRide(@PathVariable UUID id) {
        logger.info("Canceling ride: id={}", id);
        try {
            Ride ride = rideService.cancelRide(id);
            logger.info("Ride canceled successfully: id={}", id);
            return ResponseEntity.ok(ride);
        } catch (IllegalArgumentException e) {
            logger.warn("Ride not found for cancel: id={}, error={}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(e.getMessage()));
        } catch (IllegalStateException e) {
            logger.warn("Cannot cancel ride: id={}, error={}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get ride by ID
     * GET /api/rides/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getRideById(@PathVariable UUID id) {
        logger.info("Getting ride by ID: id={}", id);
        try {
            var rideOptional = rideService.getRideById(id);
            if (rideOptional.isPresent()) {
                return ResponseEntity.ok(rideOptional.get());
            } else {
                logger.warn("Ride not found: id={}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Ride not found"));
            }
        } catch (Exception e) {
            logger.error("Error getting ride: id={}, error={}", id, e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get rides by user ID
     * GET /api/rides/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Ride>> getRidesByUserId(@PathVariable UUID userId) {
        logger.info("Getting rides for user: userId={}", userId);
        List<Ride> rides = rideService.getRidesByUserId(userId);
        logger.info("Retrieved {} rides for user: userId={}", rides.size(), userId);
        return ResponseEntity.ok(rides);
    }

    /**
     * Get active rides by user ID
     * GET /api/rides/user/{userId}/active
     */
    @GetMapping("/user/{userId}/active")
    public ResponseEntity<List<Ride>> getActiveRidesByUserId(@PathVariable UUID userId) {
        logger.info("Getting active rides for user: userId={}", userId);
        List<Ride> activeRides = rideService.getActiveRidesByUserId(userId);
        logger.info("Retrieved {} active rides for user: userId={}", activeRides.size(), userId);
        return ResponseEntity.ok(activeRides);
    }

    /**
     * Get rides by vehicle ID
     * GET /api/rides/vehicle/{vehicleId}
     */
    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<List<Ride>> getRidesByVehicleId(@PathVariable UUID vehicleId) {
        logger.info("Getting rides for vehicle: vehicleId={}", vehicleId);
        List<Ride> rides = rideService.getRidesByVehicleId(vehicleId);
        logger.info("Retrieved {} rides for vehicle: vehicleId={}", rides.size(), vehicleId);
        return ResponseEntity.ok(rides);
    }

    /**
     * Get all active rides
     * GET /api/rides/active
     */
    @GetMapping("/active")
    public ResponseEntity<List<Ride>> getAllActiveRides() {
        logger.info("Getting all active rides");
        List<Ride> activeRides = rideService.getAllActiveRides();
        logger.info("Retrieved {} active rides", activeRides.size());
        return ResponseEntity.ok(activeRides);
    }

    /**
     * Get all completed rides
     * GET /api/rides/completed
     */
    @GetMapping("/completed")
    public ResponseEntity<List<Ride>> getAllCompletedRides() {
        logger.info("Getting all completed rides");
        List<Ride> completedRides = rideService.getAllCompletedRides();
        logger.info("Retrieved {} completed rides", completedRides.size());
        return ResponseEntity.ok(completedRides);
    }

    /**
     * Calculate ride cost
     * POST /api/rides/calculate-cost
     */
    @PostMapping("/calculate-cost")
    public ResponseEntity<?> calculateRideCost(@RequestBody CostCalculationRequest request) {
        logger.info("Calculating ride cost: startTime={}, endTime={}", request.startTime(), request.endTime());
        try {
            var cost = rideService.calculateRideCost(request.startTime(), request.endTime());
            logger.info("Ride cost calculated: cost={}", cost);
            return ResponseEntity.ok(new CostResponse(cost));
        } catch (Exception e) {
            logger.error("Error calculating ride cost: error={}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // Request/Response records

    public record StartRideRequest(UUID userId, UUID vehicleId, String startLocation) {}

    public record EndRideRequest(String endLocation) {}

    public record CostCalculationRequest(java.time.LocalDateTime startTime, java.time.LocalDateTime endTime) {}

    public record CostResponse(java.math.BigDecimal cost) {}

    public record ErrorResponse(String message) {}
}