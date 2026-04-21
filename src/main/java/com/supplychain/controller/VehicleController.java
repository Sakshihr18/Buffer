package com.supplychain.controller;

import com.supplychain.model.Vehicle;
import com.supplychain.service.VehicleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @GetMapping
    public ResponseEntity<List<Vehicle>> getAllVehicles() {
        return ResponseEntity.ok(vehicleService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Vehicle> getVehicle(@PathVariable String id) {
        return vehicleService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/available")
    public ResponseEntity<List<Vehicle>> getAvailableVehicles() {
        return ResponseEntity.ok(vehicleService.findAvailable());
    }

    @PostMapping
    public ResponseEntity<Vehicle> createVehicle(@RequestBody Vehicle vehicle) {
        return ResponseEntity.ok(vehicleService.save(vehicle));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Vehicle> updateVehicle(@PathVariable String id,
                                                  @RequestBody Vehicle vehicle) {
        return vehicleService.findById(id).map(existing -> {
            existing.setName(vehicle.getName());
            existing.setType(vehicle.getType());
            existing.setWeightCapacity(vehicle.getWeightCapacity());
            existing.setVolumeCapacity(vehicle.getVolumeCapacity());
            existing.setFuelLevel(vehicle.getFuelLevel());
            existing.setAvailable(vehicle.getAvailable());
            existing.setCurrentNode(vehicle.getCurrentNode());
            return ResponseEntity.ok(vehicleService.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/availability")
    public ResponseEntity<Vehicle> updateAvailability(@PathVariable String id,
                                                         @RequestBody Map<String, Boolean> payload) {
        boolean available = payload.get("available");
        return ResponseEntity.ok(vehicleService.updateAvailability(id, available));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVehicle(@PathVariable String id) {
        vehicleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}