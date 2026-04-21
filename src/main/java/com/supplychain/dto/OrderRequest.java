package com.supplychain.dto;

import com.supplychain.model.enums.Priority;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderRequest {
    private String id;
    private String customerId;
    private String productId;
    private Integer quantity;
    private BigDecimal weight;
    private BigDecimal volume;
    private BigDecimal value;
    private Priority priority;
    private String destinationNode;
    private Integer daysToDeadline;
    private String notes;
}