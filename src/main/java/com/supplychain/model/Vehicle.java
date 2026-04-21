package com.supplychain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @Column(length = 20)
    private String id;

    @Column(length = 80, nullable = false)
    private String name;

    @Column(length = 10)
    private String type = "van";

    @Column(name = "weight_capacity", precision = 8, scale = 2)
    private BigDecimal weightCapacity = new BigDecimal("500");

    @Column(name = "volume_capacity", precision = 8, scale = 3)
    private BigDecimal volumeCapacity = new BigDecimal("250");

    @Column(name = "fuel_level", precision = 5, scale = 2)
    private BigDecimal fuelLevel = new BigDecimal("100");

    @Column
    private Boolean available = true;

    @Column(name = "current_node", length = 10)
    private String currentNode = "W1";

    @Column(name = "assigned_orders", columnDefinition = "JSON")
    private String assignedOrders = "[]";

    @Column(name = "total_deliveries")
    private Integer totalDeliveries = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Vehicle() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public BigDecimal getWeightCapacity() { return weightCapacity; }
    public void setWeightCapacity(BigDecimal weightCapacity) { this.weightCapacity = weightCapacity; }
    public BigDecimal getVolumeCapacity() { return volumeCapacity; }
    public void setVolumeCapacity(BigDecimal volumeCapacity) { this.volumeCapacity = volumeCapacity; }
    public BigDecimal getFuelLevel() { return fuelLevel; }
    public void setFuelLevel(BigDecimal fuelLevel) { this.fuelLevel = fuelLevel; }
    public Boolean getAvailable() { return available; }
    public void setAvailable(Boolean available) { this.available = available; }
    public String getCurrentNode() { return currentNode; }
    public void setCurrentNode(String currentNode) { this.currentNode = currentNode; }
    public String getAssignedOrders() { return assignedOrders; }
    public void setAssignedOrders(String assignedOrders) { this.assignedOrders = assignedOrders; }
    public Integer getTotalDeliveries() { return totalDeliveries; }
    public void setTotalDeliveries(Integer totalDeliveries) { this.totalDeliveries = totalDeliveries; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}