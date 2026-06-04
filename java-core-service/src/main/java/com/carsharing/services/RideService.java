package com.carsharing.services;

import com.carsharing.entities.Ride;
import com.carsharing.entities.Vehicle;
import com.carsharing.repositories.RideRepository;
import com.carsharing.repositories.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RideService {

    private final RideRepository rideRepository;
    private final VehicleRepository vehicleRepository;
    private final UserService userService;

    private static final BigDecimal PRICE_PER_MINUTE = new BigDecimal("5.00");
    private static final BigDecimal MINIMUM_FARE = new BigDecimal("50.00");

    public RideService(RideRepository rideRepository, VehicleRepository vehicleRepository, UserService userService) {
        this.rideRepository = rideRepository;
        this.vehicleRepository = vehicleRepository;
        this.userService = userService;
    }

    @Transactional
    public Ride startRide(UUID userId, UUID vehicleId, String startLocation) {
        if (!userService.isUserActive(userId)) {
            throw new IllegalArgumentException("User not found or inactive");
        }
        
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));
        
        if (!vehicle.isAvailable()) {
            throw new IllegalStateException("Vehicle is not available for rent");
        }
        
        List<Ride> activeRides = rideRepository.findActiveRidesByUserId(userId);
        if (!activeRides.isEmpty()) {
            throw new IllegalStateException("User already has an active ride");
        }
        
        vehicle.rent();
        vehicleRepository.save(vehicle);
        
        Ride ride = new Ride(userId, vehicleId, startLocation);
        return rideRepository.save(ride);
    }

    @Transactional
    public Ride endRide(UUID rideId, String endLocation) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new IllegalArgumentException("Ride not found"));
        
        if (!ride.isActive()) {
            throw new IllegalStateException("Ride is not active");
        }
        
        LocalDateTime startTime = ride.getStartTime();
        LocalDateTime endTime = LocalDateTime.now();
        Duration duration = Duration.between(startTime, endTime);
        long minutes = duration.toMinutes();
        
        BigDecimal calculatedCost = PRICE_PER_MINUTE.multiply(BigDecimal.valueOf(minutes));
        if (calculatedCost.compareTo(MINIMUM_FARE) < 0) {
            calculatedCost = MINIMUM_FARE;
        }
        
        Vehicle vehicle = vehicleRepository.findById(ride.getVehicleId())
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));
        vehicle.returnVehicle();
        vehicleRepository.save(vehicle);
        
        ride.completeRide(endLocation, calculatedCost);
        return rideRepository.save(ride);
    }

    @Transactional
    public Ride cancelRide(UUID rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new IllegalArgumentException("Ride not found"));
        
        if (!ride.isActive()) {
            throw new IllegalStateException("Ride is not active");
        }
        
        Vehicle vehicle = vehicleRepository.findById(ride.getVehicleId())
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));
        vehicle.returnVehicle();
        vehicleRepository.save(vehicle);
        
        ride.cancelRide();
        return rideRepository.save(ride);
    }

    public Optional<Ride> getRideById(UUID rideId) {
        return rideRepository.findById(rideId);
    }

    public List<Ride> getRidesByUserId(UUID userId) {
        return rideRepository.findByUserId(userId);
    }

    public List<Ride> getActiveRidesByUserId(UUID userId) {
        return rideRepository.findActiveRidesByUserId(userId);
    }

    public List<Ride> getRidesByVehicleId(UUID vehicleId) {
        return rideRepository.findByVehicleId(vehicleId);
    }

    public List<Ride> getAllActiveRides() {
        return rideRepository.findByStatus("active");
    }

    public List<Ride> getAllCompletedRides() {
        return rideRepository.findByStatus("completed");
    }

    public List<Ride> getCompletedRidesOlderThan(LocalDateTime cutoffDate) {
        return rideRepository.findCompletedRidesOlderThan(cutoffDate);
    }

    public BigDecimal calculateRideCost(LocalDateTime startTime, LocalDateTime endTime) {
        Duration duration = Duration.between(startTime, endTime);
        long minutes = duration.toMinutes();
        
        BigDecimal calculatedCost = PRICE_PER_MINUTE.multiply(BigDecimal.valueOf(minutes));
        if (calculatedCost.compareTo(MINIMUM_FARE) < 0) {
            calculatedCost = MINIMUM_FARE;
        }
        
        return calculatedCost;
    }

    public long getRideDurationMinutes(Ride ride) {
        if (ride.getEndTime() == null) {
            return Duration.between(ride.getStartTime(), LocalDateTime.now()).toMinutes();
        }
        return Duration.between(ride.getStartTime(), ride.getEndTime()).toMinutes();
    }

    public boolean isRideEligibleForArchiving(Ride ride, int monthsThreshold) {
        if (!ride.isCompleted()) {
            return false;
        }
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusMonths(monthsThreshold);
        return ride.getCreatedAt().isBefore(cutoffDate);
    }
}