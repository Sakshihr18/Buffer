package com.supplychain.dto;

import java.util.List;

public class RouteResponse {
    private List<String> path;
    private Integer hops;
    private Integer cost;
    private String algorithm;
    private Boolean isValid;
    private Integer baseCost;
    private Integer delayPenalty;
    private Integer totalCost;

    public RouteResponse() {}

    public List<String> getPath() { return path; }
    public void setPath(List<String> path) { this.path = path; }
    public Integer getHops() { return hops; }
    public void setHops(Integer hops) { this.hops = hops; }
    public Integer getCost() { return cost; }
    public void setCost(Integer cost) { this.cost = cost; }
    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
    public Boolean getIsValid() { return isValid; }
    public void setIsValid(Boolean isValid) { this.isValid = isValid; }
    public Integer getBaseCost() { return baseCost; }
    public void setBaseCost(Integer baseCost) { this.baseCost = baseCost; }
    public Integer getDelayPenalty() { return delayPenalty; }
    public void setDelayPenalty(Integer delayPenalty) { this.delayPenalty = delayPenalty; }
    public Integer getTotalCost() { return totalCost; }
    public void setTotalCost(Integer totalCost) { this.totalCost = totalCost; }
}