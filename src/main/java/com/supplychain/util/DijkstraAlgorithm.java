package com.supplychain.util;

import com.supplychain.model.Edge;
import com.supplychain.model.Node;
import java.util.*;

public class DijkstraAlgorithm {

    public static class Result {
        public Map<String, Integer> distances = new HashMap<>();
        public Map<String, String> previous = new HashMap<>();
    }

    public static Result findShortestPaths(Graph graph, String source) {
        Result result = new Result();
        Map<String, Integer> dist = result.distances;
        Map<String, String> prev = result.previous;
        Set<String> visited = new HashSet<>();
        PriorityQueue<NodeEntry> pq = new PriorityQueue<>(Comparator.comparingInt(e -> e.distance));

        for (Node node : graph.getAllNodes()) {
            dist.put(node.getId(), Integer.MAX_VALUE);
            prev.put(node.getId(), null);
        }
        dist.put(source, 0);
        pq.add(new NodeEntry(source, 0));

        while (!pq.isEmpty()) {
            NodeEntry current = pq.poll();
            String u = current.nodeId;

            if (visited.contains(u)) continue;
            visited.add(u);

            for (Edge edge : graph.getNeighbors(u)) {
                String v = edge.getTo();
                if (visited.contains(v)) continue;

                int alt = dist.get(u) + edge.getWeight();
                if (alt < dist.get(v)) {
                    dist.put(v, alt);
                    prev.put(v, u);
                    pq.add(new NodeEntry(v, alt));
                }
            }
        }
        return result;
    }

    public static List<String> reconstructPath(Map<String, String> previous, String source, String target) {
        List<String> path = new ArrayList<>();
        String current = target;

        while (current != null) {
            path.add(0, current);
            current = previous.get(current);
        }

        if (path.isEmpty() || !path.get(0).equals(source)) {
            return new ArrayList<>();
        }
        return path;
    }

    private static class NodeEntry {
        String nodeId;
        int distance;

        NodeEntry(String nodeId, int distance) {
            this.nodeId = nodeId;
            this.distance = distance;
        }
    }
}