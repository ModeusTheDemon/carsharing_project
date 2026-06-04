package com.carsharing.services;

import com.carsharing.entities.Ride;
import com.carsharing.entities.Telemetry;
import com.carsharing.repositories.RideRepository;
import com.carsharing.repositories.TelemetryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class TelemetryService {

    private final TelemetryRepository telemetryRepository;
    private final RideRepository rideRepository;
    private final ObjectMapper objectMapper;

    public TelemetryService(TelemetryRepository telemetryRepository, RideRepository rideRepository) {
        this.telemetryRepository = telemetryRepository;
        this.rideRepository = rideRepository;
        this.objectMapper = new ObjectMapper();
    }

    @Transactional
    public Telemetry storeTelemetryData(UUID rideId, UUID vehicleId, Map<String, Object> telemetryData) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new IllegalArgumentException("Ride not found"));
        
        if (!ride.isActive()) {
            throw new IllegalStateException("Cannot store telemetry for inactive ride");
        }
        
        if (!ride.getVehicleId().equals(vehicleId)) {
            throw new IllegalArgumentException("Vehicle ID does not match ride");
        }
        
        try {
            String jsonData = objectMapper.writeValueAsString(telemetryData);
            
            Telemetry telemetry = new Telemetry(rideId, vehicleId, jsonData);
            return telemetryRepository.save(telemetry);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid telemetry data format", e);
        }
    }

    @Transactional
    public Telemetry storeTelemetryData(UUID rideId, UUID vehicleId, String jsonData) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new IllegalArgumentException("Ride not found"));
        
        if (!ride.isActive()) {
            throw new IllegalStateException("Cannot store telemetry for inactive ride");
        }
        
        if (!ride.getVehicleId().equals(vehicleId)) {
            throw new IllegalArgumentException("Vehicle ID does not match ride");
        }
        
        try {
            objectMapper.readTree(jsonData);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid JSON format", e);
        }
        
        Telemetry telemetry = new Telemetry(rideId, vehicleId, jsonData);
        return telemetryRepository.save(telemetry);
    }

    public Optional<Telemetry> getTelemetryById(UUID telemetryId) {
        return telemetryRepository.findById(telemetryId);
    }

    public List<Telemetry> getTelemetryByRideId(UUID rideId) {
        return telemetryRepository.findByRideId(rideId);
    }

    public List<Telemetry> getTelemetryByRideIdOrdered(UUID rideId) {
        return telemetryRepository.findByRideIdOrderByTimestamp(rideId);
    }

    public List<Telemetry> getTelemetryByVehicleId(UUID vehicleId) {
        return telemetryRepository.findByVehicleId(vehicleId);
    }

    public List<Telemetry> getTelemetryOlderThan(LocalDateTime cutoffDate) {
        return telemetryRepository.findOlderThan(cutoffDate);
    }

    @Transactional
    public void deleteTelemetry(UUID telemetryId) {
        telemetryRepository.deleteById(telemetryId);
    }

    @Transactional
    public int deleteTelemetryOlderThan(LocalDateTime cutoffDate) {
        List<Telemetry> oldTelemetry = telemetryRepository.findOlderThan(cutoffDate);
        telemetryRepository.deleteAll(oldTelemetry);
        return oldTelemetry.size();
    }

    public Map<String, Object> parseTelemetryData(Telemetry telemetry) {
        try {
            return objectMapper.readValue(telemetry.getData(), Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse telemetry data", e);
        }
    }

    public TelemetryStatistics getTelemetryStatisticsForRide(UUID rideId) {
        List<Telemetry> telemetryList = telemetryRepository.findByRideIdOrderByTimestamp(rideId);
        
        if (telemetryList.isEmpty()) {
            return new TelemetryStatistics(0, Map.of(), Map.of(), null, null, null);
        }
        
        Map<String, Object> firstData = parseTelemetryData(telemetryList.get(0));
        Map<String, Object> lastData = parseTelemetryData(telemetryList.get(telemetryList.size() - 1));
        
        Double averageSpeed = null;
        if (telemetryList.stream().anyMatch(t -> parseTelemetryData(t).containsKey("speed"))) {
            averageSpeed = telemetryList.stream()
                    .map(this::parseTelemetryData)
                    .filter(data -> data.containsKey("speed"))
                    .map(data -> (Double) data.get("speed"))
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);
        }
        
        Double totalDistance = null;
        if (telemetryList.stream().anyMatch(t -> parseTelemetryData(t).containsKey("distance"))) {
            totalDistance = telemetryList.stream()
                    .map(this::parseTelemetryData)
                    .filter(data -> data.containsKey("distance"))
                    .map(data -> (Double) data.get("distance"))
                    .mapToDouble(Double::doubleValue)
                    .sum();
        }
        
        Double fuelConsumption = null;
        if (telemetryList.stream().anyMatch(t -> parseTelemetryData(t).containsKey("fuel_consumption"))) {
            fuelConsumption = telemetryList.stream()
                    .map(this::parseTelemetryData)
                    .filter(data -> data.containsKey("fuel_consumption"))
                    .map(data -> (Double) data.get("fuel_consumption"))
                    .mapToDouble(Double::doubleValue)
                    .sum();
        }
        
        return new TelemetryStatistics(
                telemetryList.size(),
                firstData,
                lastData,
                averageSpeed,
                totalDistance,
                fuelConsumption
        );
    }

    public boolean isTelemetryOlderThanDays(Telemetry telemetry, int days) {
        return telemetry.isOlderThanDays(days);
    }

    private Double convertToDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Double) {
            return (Double) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).doubleValue();
        }
        if (value instanceof Long) {
            return ((Long) value).doubleValue();
        }
        if (value instanceof Float) {
            return ((Float) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    public record TelemetryStatistics(
            int totalRecords,
            Map<String, Object> firstRecord,
            Map<String, Object> lastRecord,
            Double averageSpeed,
            Double totalDistance,
            Double fuelConsumption
    ) {}
}