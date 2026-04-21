package com.supplychain.util;

import com.supplychain.model.Edge;
import com.supplychain.model.Node;
import java.util.*;

public class AStarAlgorithm {

    public static class Result {
        public List<String> path = new ArrayList<>();
        public int cost = Integer.MAX_VALUE;
    }

    public static Result findPath(Graph graph, String source, String target) {
        Result result = new Result();
        Map<String, Integer> gScore = new HashMap<>();
        Map<String, Integer> fScore = new HashMap<>();
        Map<String, String> previous = new HashMap<>();
        Set<String> visited = new HashSet<>();
        PriorityQueue<NodeEntry> pq = new PriorityQueue<>(Comparator.comparingInt(e -> e.fScore));

        for (Node node : graph.getAllNodes()) {
            String id = node.getId();
            gScore.put(id, Integer.MAX_VALUE);
            fScore.put(id, Integer.MAX_VALUE);
            previous.put(id, null);
        }

        gScore.put(source, 0);
        fScore.put(source, (int) graph.heuristic(source, target));
        pq.add(new NodeEntry(source, fScore.get(source)));

        while (!pq.isEmpty()) {
            NodeEntry current = pq.poll();
            String currentId = current.nodeId;

            if (currentId.equals(target)) {
                break;
            }
            if (visited.contains(currentId)) continue;
            visited.add(currentId);

            for (Edge edge : graph.getNeighbors(currentId)) {
                String neighbor = edge.getTo();
                if (visited.contains(neighbor)) continue;

                int tentativeG = gScore.get(currentId) + edge.getWeight();
                if (tentativeG < gScore.get(neighbor)) {
                    gScore.put(neighbor, tentativeG);
                    int f = tentativeG + (int) graph.heuristic(neighbor, target);
                    fScore.put(neighbor, f);
                    previous.put(neighbor, currentId);
                    pq.add(new NodeEntry(neighbor, f));
                }
            }
        }

        result.cost = gScore.get(target);
        String current = target;
        while (current != null) {
            result.path.add(0, current);
            current = previous.get(current);
        }

        if (result.path.isEmpty() || !result.path.get(0).equals(source)) {
            result.path.clear();
            result.cost = Integer.MAX_VALUE;
        }

        return result;
    }

    private static class NodeEntry {
        String nodeId;
        int fScore;

        NodeEntry(String nodeId, int fScore) {
            this.nodeId = nodeId;
            this.fScore = fScore;
        }
    }
}