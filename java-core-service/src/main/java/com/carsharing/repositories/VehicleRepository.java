package com.carsharing.repositories;

import com.carsharing.entities.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {
    List<Vehicle> findByStatus(String status);
    List<Vehicle> findByBrand(String brand);
    Vehicle findByPlateNumber(String plateNumber);
    List<Vehicle> findByStatusOrderByUpdatedAtDesc(String status);
}