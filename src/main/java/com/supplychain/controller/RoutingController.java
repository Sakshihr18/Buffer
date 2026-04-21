package com.supplychain.controller;

import com.supplychain.dto.RouteResponse;
import com.supplychain.model.Node;
import com.supplychain.service.RoutingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/routes")
public class RoutingController {

    private final RoutingService routingService;

    public RoutingController(RoutingService routingService) {
        this.routingService = routingService;
    }

    @GetMapping
    public ResponseEntity<List<Node>> getAllNodes() {
        return ResponseEntity.ok(routingService.getAllNodes());
    }

    @GetMapping("/optimize")
    public ResponseEntity<RouteResponse> optimizeRoute(
            @RequestParam String source,
            @RequestParam String destination,
            @RequestParam(required = false) String algorithm) {
        RouteResponse response = routingService.optimizeRoute(source, destination, algorithm);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/return")
    public ResponseEntity<RouteResponse> calculateReturnRoute(
            @RequestParam String customerNode,
            @RequestParam(required = false) String warehouseNode) {
        RouteResponse response = routingService.calculateReturnRoute(customerNode, warehouseNode);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/traffic-delay")
    public ResponseEntity<Map<String, Object>> simulateTrafficDelay(
            @RequestBody Map<String, Object> payload) {
        String fromNode = (String) payload.get("fromNode");
        String toNode = (String) payload.get("toNode");
        double multiplier = payload.get("multiplier") != null ?
            Double.parseDouble(payload.get("multiplier").toString()) : 2.5;

        Map<String, Object> result = routingService.simulateTrafficDelay(fromNode, toNode, multiplier);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/traffic-delay")
    public ResponseEntity<Void> clearDelays() {
        routingService.clearAllDelays();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/traffic-delay")
    public ResponseEntity<Map<String, Double>> getActiveDelays() {
        return ResponseEntity.ok(routingService.getActiveDelays());
    }

    @GetMapping("/all")
    public ResponseEntity<Map<String, RouteResponse>> getAllRoutes(
            @RequestParam String source) {
        return ResponseEntity.ok(routingService.getAllRoutesFrom(source));
    }
}