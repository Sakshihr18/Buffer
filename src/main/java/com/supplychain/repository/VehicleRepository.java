package com.supplychain.repository;

import com.supplychain.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, String> {
    List<Vehicle> findByAvailableTrue();
    List<Vehicle> findByCurrentNode(String node);
}