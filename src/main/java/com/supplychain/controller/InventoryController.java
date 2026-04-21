package com.supplychain.controller;

import com.supplychain.dto.InventoryAlert;
import com.supplychain.model.Inventory;
import com.supplychain.service.InventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public ResponseEntity<List<Inventory>> getAllInventory() {
        return ResponseEntity.ok(inventoryService.findAll());
    }

    @GetMapping("/{productId}")
    public ResponseEntity<Inventory> getInventory(@PathVariable String productId) {
        return inventoryService.findByProductId(productId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{productId}")
    public ResponseEntity<Inventory> updateInventory(@PathVariable String productId,
                                                  @RequestBody Map<String, Object> payload) {
        Integer quantity = payload.containsKey("quantity") ?
            Integer.parseInt(payload.get("quantity").toString()) : null;
        Integer reserved = payload.containsKey("reserved") ?
            Integer.parseInt(payload.get("reserved").toString()) : null;
        Integer maxCapacity = payload.containsKey("maxCapacity") ?
            Integer.parseInt(payload.get("maxCapacity").toString()) : null;
        Integer reorderPoint = payload.containsKey("reorderPoint") ?
            Integer.parseInt(payload.get("reorderPoint").toString()) : null;

        Inventory updated = inventoryService.updateStock(productId, quantity, reserved,
            maxCapacity, reorderPoint);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{productId}/reserve")
    public ResponseEntity<Map<String, Object>> reserveStock(
            @PathVariable String productId,
            @RequestBody Map<String, Object> payload) {
        Integer quantity = Integer.parseInt(payload.get("quantity").toString());
        boolean success = inventoryService.reserveStock(productId, quantity);

        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("productId", productId);
        response.put("quantity", quantity);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{productId}/release")
    public ResponseEntity<Void> releaseReservation(
            @PathVariable String productId,
            @RequestBody Map<String, Object> payload) {
        Integer quantity = Integer.parseInt(payload.get("quantity").toString());
        inventoryService.releaseReservation(productId, quantity);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<InventoryAlert>> getAlerts() {
        return ResponseEntity.ok(inventoryService.getAllAlerts());
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(inventoryService.getStats());
    }
}