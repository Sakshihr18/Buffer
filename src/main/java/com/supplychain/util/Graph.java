package com.supplychain.util;

import com.supplychain.model.Edge;
import com.supplychain.model.Node;
import java.util.*;

public class Graph {
    private final Map<String, Node> nodes = new HashMap<>();
    private final Map<String, List<Edge>> edges = new HashMap<>();
    private final Map<String, Map<String, Double>> trafficDelays = new HashMap<>();

    public void addNode(Node node) {
        nodes.put(node.getId(), node);
        edges.putIfAbsent(node.getId(), new ArrayList<>());
    }

    public void addEdge(String from, String to, int weight) {
        edges.putIfAbsent(from, new ArrayList<>());
        edges.putIfAbsent(to, new ArrayList<>());

        Edge forward = new Edge(from, to, weight, null);
        Edge backward = new Edge(to, from, weight, null);

        edges.get(from).add(forward);
        edges.get(to).add(backward);
    }

    public void applyTrafficDelay(String from, String to, double multiplier) {
        trafficDelays.putIfAbsent(from, new HashMap<>());
        trafficDelays.get(from).put(to, multiplier);

        trafficDelays.putIfAbsent(to, new HashMap<>());
        trafficDelays.get(to).put(from, multiplier);

        List<Edge> fromEdges = edges.get(from);
        if (fromEdges != null) {
            for (Edge e : fromEdges) {
                if (e.getTo().equals(to)) {
                    e.setWeight((int) Math.round(e.getWeight() * multiplier));
                }
            }
        }
    }

    public void clearTrafficDelays() {
        trafficDelays.clear();
    }

    public Map<String, Double> getActiveDelays() {
        Map<String, Double> result = new HashMap<>();
        for (Map.Entry<String, Map<String, Double>> entry : trafficDelays.entrySet()) {
            String from = entry.getKey();
            Map<String, Double> delays = entry.getValue();
            for (Map.Entry<String, Double> delayEntry : delays.entrySet()) {
                result.put(from + "-" + delayEntry.getKey(), delayEntry.getValue());
            }
        }
        return result;
    }

    public List<Edge> getNeighbors(String nodeId) {
        return edges.getOrDefault(nodeId, new ArrayList<>());
    }

    public Node getNode(String nodeId) {
        return nodes.get(nodeId);
    }

    public Collection<Node> getAllNodes() {
        return nodes.values();
    }

    public double heuristic(String nodeA, String nodeB) {
        Node a = nodes.get(nodeA);
        Node b = nodes.get(nodeB);
        if (a == null || b == null) return 0;
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }
}