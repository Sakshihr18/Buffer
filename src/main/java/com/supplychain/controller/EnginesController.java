package com.supplychain.controller;

import com.supplychain.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api")
public class EnginesController {

    private final FEFOService fefoService;
    private final ReplenishmentService replenishmentService;
    private final SubstitutionService substitutionService;
    private final SLAService slaService;
    private final DeliveryService deliveryService;

    public EnginesController(FEFOService fefoService, ReplenishmentService replenishmentService,
                       SubstitutionService substitutionService, SLAService slaService,
                       DeliveryService deliveryService) {
        this.fefoService = fefoService;
        this.replenishmentService = replenishmentService;
        this.substitutionService = substitutionService;
        this.slaService = slaService;
        this.deliveryService = deliveryService;
    }

    // ============ FEFO ENDPOINTS ============
    @GetMapping("/fefo/pick/{productId}")
    public ResponseEntity<Map<String, Object>> pickProduct(
            @PathVariable String productId,
            @RequestParam(defaultValue = "1") int quantity,
            @RequestParam(defaultValue = "WH001") String warehouseId) {
        FEFOService.PickingResult result = fefoService.getPickingRecommendation(productId, quantity, warehouseId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", result.success);
        response.put("message", result.message);
        response.put("totalQuantity", result.totalQuantity);
        response.put("batches", result.batches);
        response.put("warnings", result.warnings);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/fefo/alerts")
    public ResponseEntity<List<FEFOService.ExpiryAlert>> getExpiryAlerts(
            @RequestParam(defaultValue = "WH001") String warehouseId,
            @RequestParam(defaultValue = "72") int thresholdHours) {
        return ResponseEntity.ok(fefoService.getExpiryAlerts(warehouseId, thresholdHours));
    }

    @GetMapping("/fefo/stats")
    public ResponseEntity<Map<String, Object>> getFEFOStats(
            @RequestParam(defaultValue = "WH001") String warehouseId) {
        return ResponseEntity.ok(fefoService.getFEFOStats(warehouseId));
    }

    // ============ REPLENISHMENT ENDPOINTS ============
    @GetMapping("/replenishment/queue")
    public ResponseEntity<List<ReplenishmentService.ReorderItem>> getReorderQueue(
            @RequestParam(defaultValue = "WH001") String warehouseId) {
        return ResponseEntity.ok(replenishmentService.getReorderQueue(warehouseId));
    }

    @GetMapping("/replenishment/forecast/{productId}")
    public ResponseEntity<ReplenishmentService.DemandForecast> getForecast(
            @PathVariable String productId,
            @RequestParam(defaultValue = "WH001") String warehouseId) {
        return ResponseEntity.ok(replenishmentService.predictStockout(productId, warehouseId));
    }

    @GetMapping("/replenishment/stats")
    public ResponseEntity<Map<String, Object>> getReplenishmentStats(
            @RequestParam(defaultValue = "WH001") String warehouseId) {
        return ResponseEntity.ok(replenishmentService.getReplenishmentStats(warehouseId));
    }

    // ============ SUBSTITUTION ENDPOINTS ============
    @GetMapping("/substitution/check/{productId}")
    public ResponseEntity<Map<String, Object>> checkSubstitution(
            @PathVariable String productId,
            @RequestParam(defaultValue = "1") int quantity) {
        SubstitutionService.SubstitutionResult result = substitutionService.checkSubstitution(productId, quantity);
        Map<String, Object> response = new HashMap<>();
        response.put("requestedProductId", result.requestedProductId);
        response.put("requestedProductName", result.requestedProductName);
        response.put("isAvailable", result.isAvailable);
        response.put("message", result.message);
        response.put("primaryRecommendation", result.primaryRecommendation);
        response.put("substitutes", result.substitutes);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/substitution/out-of-stock")
    public ResponseEntity<List<SubstitutionService.SubstituteProduct>> getOutOfStock(
            @RequestParam(defaultValue = "WH001") String warehouseId) {
        return ResponseEntity.ok(substitutionService.getAllOutOfStockWithSubstitutes(warehouseId));
    }

    @GetMapping("/substitution/stats")
    public ResponseEntity<Map<String, Object>> getSubstitutionStats(
            @RequestParam(defaultValue = "WH001") String warehouseId) {
        return ResponseEntity.ok(substitutionService.getSubstitutionStats(warehouseId));
    }

    // ============ SLA ENDPOINTS ============
    @GetMapping("/sla/orders")
    public ResponseEntity<List<SLAService.SLAOrder>> getActiveOrders(
            @RequestParam(defaultValue = "WH001") String warehouseId) {
        return ResponseEntity.ok(slaService.getActiveOrdersWithSLA(warehouseId));
    }

    @GetMapping("/sla/dashboard")
    public ResponseEntity<Map<String, Object>> getSLADashboard() {
        return ResponseEntity.ok(slaService.getSLADashboard());
    }

    @PostMapping("/sla/update-scores")
    public ResponseEntity<Map<String, String>> updateScores() {
        slaService.updateSLAScores();
        return ResponseEntity.ok(Map.of("status", "Updated"));
    }

    // ============ DELIVERY ENDPOINTS ============
    @GetMapping("/delivery/find-riders/{orderId}")
    public ResponseEntity<Map<String, Object>> findRiders(
            @PathVariable String orderId,
            @RequestParam(defaultValue = "3") int limit) {
        DeliveryService.DeliveryAssignment result = deliveryService.getOptimalAssignment(orderId);
        Map<String, Object> response = new HashMap<>();
        response.put("orderId", result.orderId);
        response.put("bestRider", result.bestRider);
        response.put("alternatives", result.alternatives);
        response.put("estimatedTime", result.estimatedDeliveryTime);
        response.put("recommendation", result.routeRecommendation);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/delivery/dashboard")
    public ResponseEntity<Map<String, Object>> getDeliveryDashboard(
            @RequestParam(defaultValue = "WH001") String warehouseId) {
        return ResponseEntity.ok(deliveryService.getDeliveryDashboard(warehouseId));
    }

    @PostMapping("/delivery/assign")
    public ResponseEntity<Map<String, Object>> assignRider(
            @RequestBody Map<String, String> payload) {
        String orderId = payload.get("orderId");
        String riderId = payload.get("riderId");
        boolean success = deliveryService.assignRiderToOrder(orderId, riderId);
        return ResponseEntity.ok(Map.of("success", success, "orderId", orderId, "riderId", riderId));
    }
}