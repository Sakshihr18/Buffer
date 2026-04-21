package com.supplychain.dto;

import lombok.Data;
import java.util.List;

@Data
public class KnapsackAssignment {
    private String vehicleId;
    private String vehicleName;
    private List<String> orderIds;
    private Integer maxValue;
    private Integer usedWeight;
    private Integer usedVolume;
    private Integer weightCapacity;
    private Integer volumeCapacity;
    private Integer efficiency;
}