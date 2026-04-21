package com.supplychain.controller;

import com.supplychain.model.Order;
import com.supplychain.model.enums.OrderStatus;
import com.supplychain.model.enums.Priority;
import com.supplychain.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable String id) {
        return orderService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Map<String, Object> payload) {
        String customerId = (String) payload.get("customerId");
        String productId = (String) payload.get("productId");
        Integer quantity = payload.get("quantity") != null ?
            Integer.parseInt(payload.get("quantity").toString()) : 1;

        BigDecimal weight = payload.get("weight") != null ?
            new BigDecimal(payload.get("weight").toString()) : BigDecimal.ZERO;
        BigDecimal volume = payload.get("volume") != null ?
            new BigDecimal(payload.get("volume").toString()) : BigDecimal.ZERO;
        BigDecimal value = payload.get("value") != null ?
            new BigDecimal(payload.get("value").toString()) : BigDecimal.ZERO;

        Priority priority = payload.get("priority") != null ?
            Priority.valueOf(payload.get("priority").toString()) : Priority.MEDIUM;
        String destinationNode = (String) payload.get("destinationNode");
        Integer daysToDeadline = payload.get("daysToDeadline") != null ?
            Integer.parseInt(payload.get("daysToDeadline").toString()) : 3;
        String notes = (String) payload.get("notes");

        Order order = orderService.createOrder(customerId, productId, quantity,
            weight, volume, value, priority, destinationNode, daysToDeadline, notes);

        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Order> updateOrder(@PathVariable String id,
                                         @RequestBody Map<String, Object> payload) {
        return orderService.findById(id).map(existing -> {
            if (payload.containsKey("customerId")) {
                existing.setCustomerId((String) payload.get("customerId"));
            }
            if (payload.containsKey("productId")) {
                existing.setProductId((String) payload.get("productId"));
            }
            if (payload.containsKey("quantity")) {
                existing.setQuantity(Integer.parseInt(payload.get("quantity").toString()));
            }
            if (payload.containsKey("weight")) {
                existing.setTotalWeight(new BigDecimal(payload.get("weight").toString()));
            }
            if (payload.containsKey("volume")) {
                existing.setTotalVolume(new BigDecimal(payload.get("volume").toString()));
            }
            if (payload.containsKey("value")) {
                existing.setTotalValue(new BigDecimal(payload.get("value").toString()));
            }
            if (payload.containsKey("priority")) {
                existing.setPriority(Priority.valueOf(payload.get("priority").toString()));
            }
            if (payload.containsKey("status")) {
                existing.setStatus(OrderStatus.valueOf(payload.get("status").toString()));
            }
            if (payload.containsKey("destinationNode")) {
                existing.setDestinationNode((String) payload.get("destinationNode"));
            }
            if (payload.containsKey("deliveryCost")) {
                existing.setDeliveryCost(new BigDecimal(payload.get("deliveryCost").toString()));
            }
            if (payload.containsKey("assignedVehicle")) {
                existing.setAssignedVehicle((String) payload.get("assignedVehicle"));
            }
            if (payload.containsKey("assignedAgent")) {
                existing.setAssignedAgent((String) payload.get("assignedAgent"));
            }
            if (payload.containsKey("failReason")) {
                existing.setFailReason((String) payload.get("failReason"));
            }
            if (payload.containsKey("notes")) {
                existing.setNotes((String) payload.get("notes"));
            }

            Order updated = orderService.updateOrder(id, existing);
            return ResponseEntity.ok(updated);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable String id) {
        orderService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        return ResponseEntity.ok(orderService.getStats());
    }
}