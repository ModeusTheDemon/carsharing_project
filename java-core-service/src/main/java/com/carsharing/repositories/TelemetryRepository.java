package com.carsharing.repositories;

import com.carsharing.entities.Telemetry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TelemetryRepository extends JpaRepository<Telemetry, UUID> {
    List<Telemetry> findByRideId(UUID rideId);
    List<Telemetry> findByVehicleId(UUID vehicleId);
    
    @Query("SELECT t FROM Telemetry t WHERE t.timestamp < :cutoffDate")
    List<Telemetry> findOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    @Query("SELECT t FROM Telemetry t WHERE t.rideId = :rideId ORDER BY t.timestamp ASC")
    List<Telemetry> findByRideIdOrderByTimestamp(@Param("rideId") UUID rideId);
}