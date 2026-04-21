package com.supplychain.model;

public class Node {
    private String id;
    private String name;
    private float x;
    private float y;
    private String type;

    public Node() {}

    public Node(String id, String name, float x, float y, String type) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
        this.type = type;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public float getX() { return x; }
    public void setX(float x) { this.x = x; }
    public float getY() { return y; }
    public void setY(float y) { this.y = y; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}