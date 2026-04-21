package com.supplychain.service;

import com.supplychain.model.Vehicle;
import com.supplychain.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    public List<Vehicle> findAll() {
        return vehicleRepository.findAll();
    }

    public Optional<Vehicle> findById(String id) {
        return vehicleRepository.findById(id);
    }

    public List<Vehicle> findAvailable() {
        return vehicleRepository.findByAvailableTrue();
    }

    public Vehicle save(Vehicle vehicle) {
        return vehicleRepository.save(vehicle);
    }

    public Vehicle updateAvailability(String id, boolean available) {
        Vehicle vehicle = vehicleRepository.findById(id).orElseThrow();
        vehicle.setAvailable(available);
        return vehicleRepository.save(vehicle);
    }

    public void delete(String id) {
        vehicleRepository.deleteById(id);
    }
}