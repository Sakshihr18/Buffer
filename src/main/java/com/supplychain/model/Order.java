package com.supplychain.model;

import com.supplychain.model.enums.OrderStatus;
import com.supplychain.model.enums.Priority;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @Column(length = 20)
    private String id;

    @Column(name = "customer_id", length = 20, nullable = false)
    private String customerId;

    @Transient
    private String customerName;

    @Column(name = "product_id", length = 20, nullable = false)
    private String productId;

    @Transient
    private String productName;

    @Column
    private Integer quantity = 1;

    @Column(name = "total_weight", precision = 8, scale = 2)
    private BigDecimal totalWeight = BigDecimal.ZERO;

    @Column(name = "total_volume", precision = 8, scale = 3)
    private BigDecimal totalVolume = BigDecimal.ZERO;

    @Column(name = "total_value", precision = 10, scale = 2)
    private BigDecimal totalValue = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Priority priority = Priority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "destination_node", length = 10)
    private String destinationNode;

    @Column
    private LocalDateTime deadline;

    @Column(name = "assigned_vehicle", length = 20)
    private String assignedVehicle;

    @Column(name = "assigned_agent", length = 20)
    private String assignedAgent;

    @Column(name = "delivery_cost", precision = 10, scale = 2)
    private BigDecimal deliveryCost = BigDecimal.ZERO;

    @Column(name = "fail_reason", columnDefinition = "TEXT")
    private String failReason;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Order() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getTotalWeight() { return totalWeight; }
    public void setTotalWeight(BigDecimal totalWeight) { this.totalWeight = totalWeight; }
    public BigDecimal getTotalVolume() { return totalVolume; }
    public void setTotalVolume(BigDecimal totalVolume) { this.totalVolume = totalVolume; }
    public BigDecimal getTotalValue() { return totalValue; }
    public void setTotalValue(BigDecimal totalValue) { this.totalValue = totalValue; }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public String getDestinationNode() { return destinationNode; }
    public void setDestinationNode(String destinationNode) { this.destinationNode = destinationNode; }
    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }
    public String getAssignedVehicle() { return assignedVehicle; }
    public void setAssignedVehicle(String assignedVehicle) { this.assignedVehicle = assignedVehicle; }
    public String getAssignedAgent() { return assignedAgent; }
    public void setAssignedAgent(String assignedAgent) { this.assignedAgent = assignedAgent; }
    public BigDecimal getDeliveryCost() { return deliveryCost; }
    public void setDeliveryCost(BigDecimal deliveryCost) { this.deliveryCost = deliveryCost; }
    public String getFailReason() { return failReason; }
    public void setFailReason(String failReason) { this.failReason = failReason; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}