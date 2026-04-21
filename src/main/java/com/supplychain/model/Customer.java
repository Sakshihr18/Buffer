package com.supplychain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @Column(length = 20)
    private String id;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(length = 100)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "zone_node", length = 10)
    private String zoneNode;

    @Column(name = "x_coord")
    private Float xCoord = 0f;

    @Column(name = "y_coord")
    private Float yCoord = 0f;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Customer() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getZoneNode() { return zoneNode; }
    public void setZoneNode(String zoneNode) { this.zoneNode = zoneNode; }
    public Float getXCoord() { return xCoord; }
    public void setXCoord(Float xCoord) { this.xCoord = xCoord; }
    public Float getYCoord() { return yCoord; }
    public void setYCoord(Float yCoord) { this.yCoord = yCoord; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}