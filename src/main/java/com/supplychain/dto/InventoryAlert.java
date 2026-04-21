package com.supplychain.dto;

public class InventoryAlert {
    private String productId;
    private String productName;
    private Integer quantity;
    private String type;
    private String message;
    private String severity;

    public InventoryAlert() {}

    public InventoryAlert(String productId, String productName, Integer quantity, String type, String message, String severity) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.type = type;
        this.message = message;
        this.severity = severity;
    }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
}