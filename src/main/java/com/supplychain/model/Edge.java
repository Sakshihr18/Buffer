package com.supplychain.model;

public class Edge {
    private String from;
    private String to;
    private int weight;
    private String road;

    public Edge() {}

    public Edge(String from, String to, int weight, String road) {
        this.from = from;
        this.to = to;
        this.weight = weight;
        this.road = road;
    }

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }
    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }
    public int getWeight() { return weight; }
    public void setWeight(int weight) { this.weight = weight; }
    public String getRoad() { return road; }
    public void setRoad(String road) { this.road = road; }
}