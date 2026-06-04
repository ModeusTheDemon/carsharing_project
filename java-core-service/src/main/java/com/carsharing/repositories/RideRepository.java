package com.carsharing.repositories;

import com.carsharing.entities.Ride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface RideRepository extends JpaRepository<Ride, UUID> {
    List<Ride> findByUserId(UUID userId);
    List<Ride> findByVehicleId(UUID vehicleId);
    List<Ride> findByStatus(String status);
    
    @Query("SELECT r FROM Ride r WHERE r.userId = :userId AND r.status = 'active'")
    List<Ride> findActiveRidesByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT r FROM Ride r WHERE r.createdAt < :cutoffDate AND r.status = 'completed'")
    List<Ride> findCompletedRidesOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    @Query("SELECT r FROM Ride r WHERE r.userId = :userId AND r.status = 'completed' ORDER BY r.endTime DESC")
    List<Ride> findCompletedRidesByUserIdOrderByEndTimeDesc(@Param("userId") UUID userId);
}