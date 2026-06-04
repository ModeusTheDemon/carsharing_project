package com.carsharing.testutils;

import com.carsharing.entities.Payment;
import com.carsharing.entities.Ride;
import com.carsharing.entities.Telemetry;
import com.carsharing.entities.User;
import com.carsharing.entities.Vehicle;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class TestFixtures {

    public static String validUserRegistration() {
        return String.format(
                "{\"email\":\"%s\",\"phone\":\"%s\",\"first_name\":\"%s\",\"last_name\":\"%s\"}",
                "test@example.com", "+79001234567", "Test", "User"
        );
    }

    public static String rideStartPayload(UUID vehicleId) {
        return String.format(
                "{\"vehicle_id\":\"%s\",\"start_location\":{\"lat\":55.752020,\"lon\":37.617499}}",
                vehicleId
        );
    }

    public static String rideEndPayload() {
        return "{\"end_location\":{\"lat\":55.752020,\"lon\":37.617499}}";
    }

    public static String telemetryPayload() {
        return "{\"speed\":60.5,\"fuel\":80.0,\"lat\":55.752020,\"lon\":37.617499}";
    }

    public static String invalidEmptyUserRegistration() {
        return "{}";
    }

    public static String invalidUserRegistrationMissingEmail() {
        return "{\"phone\":\"+79001234567\",\"first_name\":\"Test\",\"last_name\":\"User\"}";
    }

    public static User createUserForArchiving() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("archived@example.com");
        user.setCreatedAt(LocalDateTime.now().minusMonths(7));
        return user;
    }

    public static Ride createRideForArchiving(User user, Vehicle vehicle) {
        Ride ride = new Ride();
        ride.setId(UUID.randomUUID());
        ride.setUserId(user.getId());
        ride.setVehicleId(vehicle.getId());
        ride.setStartTime(LocalDateTime.now().minusMonths(8));
        ride.setEndTime(LocalDateTime.now().minusMonths(7));
        ride.setStatus("completed");
        ride.setTotalCost(new BigDecimal("150.00"));
        return ride;
    }

    public static Telemetry createTelemetryForCleanup(Ride ride, Vehicle vehicle) {
        Telemetry telemetry = new Telemetry();
        telemetry.setId(UUID.randomUUID());
        telemetry.setRideId(ride.getId());
        telemetry.setVehicleId(vehicle.getId());
        telemetry.setTimestamp(LocalDateTime.now().minusDays(35));
        telemetry.setData("{\"speed\":50.0,\"lat\":55.752020,\"lon\":37.617499}");
        return telemetry;
    }

    public static Payment createRecentPayment(Ride ride, User user) {
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setRideId(ride.getId());
        payment.setUserId(user.getId());
        payment.setAmount(new BigDecimal("250.00"));
        payment.setCurrency("RUB");
        payment.setStatus("completed");
        return payment;
    }

    public static User createDeletedUserForAnonymization() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("anonymize@example.com");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setPhone("+79001234567");
        user.setIsDeleted(true);
        user.setDeletedAt(LocalDateTime.now().minusDays(1));
        return user;
    }
}