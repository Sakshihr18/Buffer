package com.supplychain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @Column(length = 20)
    private String id;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(length = 50)
    private String category;

    @Column(name = "unit_price", precision = 10, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "weight_kg", precision = 8, scale = 2)
    private BigDecimal weightKg = BigDecimal.ZERO;

    @Column(name = "volume_m3", precision = 8, scale = 3)
    private BigDecimal volumeM3 = BigDecimal.ZERO;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Transient
    private List<SubstituteProduct> substitutes;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public static class SubstituteProduct {
        private String productId;
        private String productName;
        private Double similarityScore;
        private Integer priorityRank;

        public SubstituteProduct() {}

        public SubstituteProduct(String productId, String productName, Double similarityScore, Integer priorityRank) {
            this.productId = productId;
            this.productName = productName;
            this.similarityScore = similarityScore;
            this.priorityRank = priorityRank;
        }

        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public Double getSimilarityScore() { return similarityScore; }
        public void setSimilarityScore(Double similarityScore) { this.similarityScore = similarityScore; }
        public Integer getPriorityRank() { return priorityRank; }
        public void setPriorityRank(Integer priorityRank) { this.priorityRank = priorityRank; }
    }

    public Product() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getWeightKg() { return weightKg; }
    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }
    public BigDecimal getVolumeM3() { return volumeM3; }
    public void setVolumeM3(BigDecimal volumeM3) { this.volumeM3 = volumeM3; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    public List<SubstituteProduct> getSubstitutes() { return substitutes; }
    public void setSubstitutes(List<SubstituteProduct> substitutes) { this.substitutes = substitutes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}