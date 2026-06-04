package com.carsharing.testutils;

import com.carsharing.entities.Payment;
import com.carsharing.entities.Ride;
import com.carsharing.entities.Telemetry;
import com.carsharing.entities.User;
import com.carsharing.entities.Vehicle;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Random;

public class TestDataGenerator {

    private static final Random random = new Random();

    public static User generateRandomUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user" + random.nextInt(100000) + "@example.com");
        user.setPhone("+7900" + String.format("%09d", random.nextInt(1000000000)));
        user.setFirstName("FirstName" + random.nextInt(1000));
        user.setLastName("LastName" + random.nextInt(1000));
        user.setIsDeleted(false);
        return user;
    }

    public static Vehicle generateRandomVehicle() {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(UUID.randomUUID());
        vehicle.setPlateNumber("A" + random.nextInt(999) + "BC" + random.nextInt(99));
        vehicle.setBrand("Brand" + random.nextInt(10));
        vehicle.setModel("Model" + random.nextInt(20));
        vehicle.setStatus("available");
        return vehicle;
    }

    public static Ride generateRandomRide(User user, Vehicle vehicle) {
        Ride ride = new Ride();
        ride.setId(UUID.randomUUID());
        ride.setUserId(user.getId());
        ride.setVehicleId(vehicle.getId());
        ride.setStartTime(LocalDateTime.now().minusHours(random.nextInt(24)));
        ride.setStartLocation(generateRandomLocation());
        ride.setStatus("active");
        return ride;
    }

    public static Payment generateRandomPayment(Ride ride, User user) {
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setRideId(ride.getId());
        payment.setUserId(user.getId());
        payment.setAmount(BigDecimal.valueOf(Math.abs(random.nextDouble() * 1000)).setScale(2, RoundingMode.HALF_UP));
        payment.setCurrency("RUB");
        payment.setStatus("completed");
        return payment;
    }

    public static Telemetry generateRandomTelemetry(Ride ride, Vehicle vehicle) {
        Telemetry telemetry = new Telemetry();
        telemetry.setId(UUID.randomUUID());
        telemetry.setRideId(ride.getId());
        telemetry.setVehicleId(vehicle.getId());
        telemetry.setTimestamp(LocalDateTime.now());
        telemetry.setData(generateRandomTelemetryData());
        return telemetry;
    }

    public static String generateRandomLocation() {
        double latitude = 55.0 + random.nextDouble() * 5;
        double longitude = 37.0 + random.nextDouble() * 5;
        return String.format("{\"lat\":%.6f,\"lon\":%.6f}", latitude, longitude);
    }

    public static String generateRandomTelemetryData() {
        double speed = random.nextDouble() * 120;
        double fuel = random.nextDouble() * 100;
        double latitude = 55.0 + random.nextDouble() * 5;
        double longitude = 37.0 + random.nextDouble() * 5;
        return String.format("{\"speed\":%.2f,\"fuel\":%.2f,\"lat\":%.6f,\"lon\":%.6f,\"timestamp\":\"%s\"}",
                speed, fuel, latitude, longitude, LocalDateTime.now().toString());
    }

    public static User generateDeletedUser() {
        User user = generateRandomUser();
        user.setIsDeleted(true);
        user.setDeletedAt(LocalDateTime.now().minusDays(7));
        return user;
    }

    public static Ride generateCompletedRide(User user, Vehicle vehicle) {
        Ride ride = generateRandomRide(user, vehicle);
        ride.setStatus("completed");
        ride.setEndTime(LocalDateTime.now());
        ride.setEndLocation(generateRandomLocation());
        ride.setTotalCost(BigDecimal.valueOf(Math.abs(random.nextDouble() * 500)).setScale(2, RoundingMode.HALF_UP));
        return ride;
    }
}