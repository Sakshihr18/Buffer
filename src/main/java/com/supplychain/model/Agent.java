package com.supplychain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_agents")
public class Agent {

    @Id
    @Column(length = 20)
    private String id;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(length = 20)
    private String phone;

    @Column(name = "x_coord")
    private Float xCoord = 0f;

    @Column(name = "y_coord")
    private Float yCoord = 0f;

    @Column
    private Boolean available = true;

    @Column(precision = 3, scale = 2)
    private BigDecimal rating = new BigDecimal("4.5");

    @Column(name = "total_deliveries")
    private Integer totalDeliveries = 0;

    @Column(name = "current_order", length = 20)
    private String currentOrder;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Agent() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Float getXCoord() { return xCoord; }
    public void setXCoord(Float xCoord) { this.xCoord = xCoord; }
    public Float getYCoord() { return yCoord; }
    public void setYCoord(Float yCoord) { this.yCoord = yCoord; }
    public Boolean getAvailable() { return available; }
    public void setAvailable(Boolean available) { this.available = available; }
    public BigDecimal getRating() { return rating; }
    public void setRating(BigDecimal rating) { this.rating = rating; }
    public Integer getTotalDeliveries() { return totalDeliveries; }
    public void setTotalDeliveries(Integer totalDeliveries) { this.totalDeliveries = totalDeliveries; }
    public String getCurrentOrder() { return currentOrder; }
    public void setCurrentOrder(String currentOrder) { this.currentOrder = currentOrder; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}