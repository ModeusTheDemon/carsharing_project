package com.carsharing.controllers;

import com.carsharing.entities.Vehicle;
import com.carsharing.entities.Ride;
import com.carsharing.entities.User;
import com.carsharing.services.CarsharingService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Legacy controller for backward compatibility.
 * New applications should use the specific controllers (UserController, RideController, etc.)
 */
@RestController
@RequestMapping("/api/legacy")
public class CarsharingController {

    private final CarsharingService carsharingService;

    public CarsharingController(CarsharingService carsharingService) {
        this.carsharingService = carsharingService;
    }

    @PostMapping("/users")
    public User registerUser(@RequestParam String name, @RequestParam String email) {
        return carsharingService.registerUser(name, email);
    }

    @DeleteMapping("/users/{id}")
    public String deleteUser(@PathVariable Long id) {
        carsharingService.deleteUser(id);
        return "User marked as deleted. Personal data will be cleaned by DLM worker.";
    }

    @PostMapping("/cars")
    public Vehicle addCar(@RequestParam String brand, @RequestParam String model, @RequestParam String plate) {
        return carsharingService.addCar(brand, model, plate);
    }

    @PostMapping("/rides/start")
    public Ride startRide(@RequestParam Long userId, @RequestParam Long carId) {
        return carsharingService.startRide(userId, carId);
    }

    @PostMapping("/rides/{id}/finish")
    public Ride finishRide(@PathVariable Long id, @RequestParam String reason) {
        return carsharingService.finishRide(id, reason);
    }

    // Endpoint for generating historical data (for archiving testing)
    // Note: This is deprecated and for testing purposes only
    @PostMapping("/rides/create-history")
    public Ride createHistory(@RequestParam Long userId, @RequestParam Long carId, @RequestParam int monthsAgo) {
        throw new UnsupportedOperationException(
            "This endpoint is deprecated. Use RideService with appropriate timestamps instead."
        );
    }
    
    // Health check endpoint
    @GetMapping("/health")
    public String healthCheck() {
        return "Carsharing service is running";
    }
}