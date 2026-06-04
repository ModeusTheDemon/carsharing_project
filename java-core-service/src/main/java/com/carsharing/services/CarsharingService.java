package com.carsharing.services;

import com.carsharing.entities.Ride;
import com.carsharing.entities.User;
import com.carsharing.entities.Vehicle;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Legacy service that delegates to the new specialized services.
 * This is maintained for backward compatibility with existing controllers.
 */
@Service
public class CarsharingService {

    private final UserService userService;
    private final VehicleService vehicleService;
    private final RideService rideService;
    private final PaymentService paymentService;
    private final TelemetryService telemetryService;

    public CarsharingService(UserService userService, VehicleService vehicleService, 
                            RideService rideService, PaymentService paymentService,
                            TelemetryService telemetryService) {
        this.userService = userService;
        this.vehicleService = vehicleService;
        this.rideService = rideService;
        this.paymentService = paymentService;
        this.telemetryService = telemetryService;
    }

    // Legacy method for user registration
    public User registerUser(String name, String email) {
        // Parse name into first and last name
        String[] nameParts = name.split(" ", 2);
        String firstName = nameParts.length > 0 ? nameParts[0] : "";
        String lastName = nameParts.length > 1 ? nameParts[1] : "";
        
        return userService.registerUser(email, null, firstName, lastName, "default_password");
    }

    // Legacy method for user deletion (soft delete)
    public void deleteUser(Long userId) {
        userService.deleteUser(UUID.fromString(userId.toString()));
    }

    // Legacy method for adding a vehicle
    public Vehicle addCar(String brand, String model, String plateNumber) {
        return vehicleService.addVehicle(brand, model, plateNumber, "white");
    }

    // Legacy method for starting a ride
    public Ride startRide(Long userId, Long carId) {
        // Default start location
        String startLocation = "{\"latitude\": 55.7558, \"longitude\": 37.6173, \"address\": \"Moscow\"}";
        return rideService.startRide(
            UUID.fromString(userId.toString()), 
            UUID.fromString(carId.toString()), 
            startLocation
        );
    }

    // Legacy method for finishing a ride
    public Ride finishRide(Long rideId, String reason) {
        // Default end location
        String endLocation = "{\"latitude\": 55.7600, \"longitude\": 37.6200, \"address\": \"Moscow Center\"}";
        return rideService.endRide(UUID.fromString(rideId.toString()), endLocation);
    }

    // Legacy method for creating historical rides (for testing archiving)
    public Ride createHistoricalRide(Long userId, Long carId, int monthsAgo) {
        // This method is for testing purposes only
        // In the new architecture, historical rides should be created through normal ride flow
        // with appropriate timestamps
        throw new UnsupportedOperationException(
            "createHistoricalRide is deprecated. Use RideService with appropriate timestamps instead."
        );
    }
}