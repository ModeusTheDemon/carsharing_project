package com.carsharing.controllers;

import com.carsharing.entities.Telemetry;
import com.carsharing.services.TelemetryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/telemetry")
public class TelemetryController {

    private final TelemetryService telemetryService;

    public TelemetryController(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    /**
     * Store telemetry data for an active ride
     * POST /api/telemetry
     * Validates: Requirements 2.3
     */
    @PostMapping
    public ResponseEntity<?> storeTelemetryData(@RequestBody StoreTelemetryRequest request) {
        try {
            Telemetry telemetry = telemetryService.storeTelemetryData(
                request.rideId(),
                request.vehicleId(),
                request.telemetryData()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(telemetry);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Store telemetry data with JSON string
     * POST /api/telemetry/json
     */
    @PostMapping("/json")
    public ResponseEntity<?> storeTelemetryDataJson(@RequestBody StoreTelemetryJsonRequest request) {
        try {
            Telemetry telemetry = telemetryService.storeTelemetryData(
                request.rideId(),
                request.vehicleId(),
                request.jsonData()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(telemetry);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get telemetry by ID
     * GET /api/telemetry/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getTelemetryById(@PathVariable UUID id) {
        try {
            var telemetryOptional = telemetryService.getTelemetryById(id);
            if (telemetryOptional.isPresent()) {
                return ResponseEntity.ok(telemetryOptional.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Telemetry data not found"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get telemetry by ride ID
     * GET /api/telemetry/ride/{rideId}
     */
    @GetMapping("/ride/{rideId}")
    public ResponseEntity<List<Telemetry>> getTelemetryByRideId(@PathVariable UUID rideId) {
        List<Telemetry> telemetryList = telemetryService.getTelemetryByRideId(rideId);
        return ResponseEntity.ok(telemetryList);
    }

    /**
     * Get telemetry by ride ID ordered by timestamp
     * GET /api/telemetry/ride/{rideId}/ordered
     */
    @GetMapping("/ride/{rideId}/ordered")
    public ResponseEntity<List<Telemetry>> getTelemetryByRideIdOrdered(@PathVariable UUID rideId) {
        List<Telemetry> telemetryList = telemetryService.getTelemetryByRideIdOrdered(rideId);
        return ResponseEntity.ok(telemetryList);
    }

    /**
     * Get telemetry by vehicle ID
     * GET /api/telemetry/vehicle/{vehicleId}
     */
    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<List<Telemetry>> getTelemetryByVehicleId(@PathVariable UUID vehicleId) {
        List<Telemetry> telemetryList = telemetryService.getTelemetryByVehicleId(vehicleId);
        return ResponseEntity.ok(telemetryList);
    }

    /**
     * Parse telemetry data
     * GET /api/telemetry/{id}/parse
     */
    @GetMapping("/{id}/parse")
    public ResponseEntity<?> parseTelemetryData(@PathVariable UUID id) {
        try {
            var telemetryOptional = telemetryService.getTelemetryById(id);
            if (telemetryOptional.isPresent()) {
                Map<String, Object> parsedData = telemetryService.parseTelemetryData(telemetryOptional.get());
                return ResponseEntity.ok(parsedData);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Telemetry data not found"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get telemetry statistics for a ride
     * GET /api/telemetry/ride/{rideId}/statistics
     */
    @GetMapping("/ride/{rideId}/statistics")
    public ResponseEntity<?> getTelemetryStatisticsForRide(@PathVariable UUID rideId) {
        try {
            var statistics = telemetryService.getTelemetryStatisticsForRide(rideId);
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Delete telemetry data
     * DELETE /api/telemetry/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTelemetry(@PathVariable UUID id) {
        try {
            telemetryService.deleteTelemetry(id);
            return ResponseEntity.ok(new MessageResponse("Telemetry data deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // Request/Response records

    public record StoreTelemetryRequest(UUID rideId, UUID vehicleId, Map<String, Object> telemetryData) {}

    public record StoreTelemetryJsonRequest(UUID rideId, UUID vehicleId, String jsonData) {}

    public record ErrorResponse(String message) {}

    public record MessageResponse(String message) {}
}