package com.supplychain.service;

import com.supplychain.dto.RouteResponse;
import com.supplychain.model.Node;
import com.supplychain.util.AStarAlgorithm;
import com.supplychain.util.DijkstraAlgorithm;
import com.supplychain.util.Graph;
import com.supplychain.util.NetworkInitializer;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class RoutingService {

    private final NetworkInitializer networkInitializer;
    private static final int BASE_COST_PER_KM = 2;
    private static final int DELAY_PENALTY_PER_HOUR = 5;

    public RoutingService(NetworkInitializer networkInitializer) {
        this.networkInitializer = networkInitializer;
    }

    public Graph getGraph() {
        return networkInitializer.getGraph();
    }

    public List<Node> getAllNodes() {
        return new ArrayList<>(getGraph().getAllNodes());
    }

    public RouteResponse optimizeRoute(String source, String destination, String algorithm) {
        Graph graph = getGraph();

        if (algorithm == null) {
            algorithm = "dijkstra";
        }

        List<String> path;
        int cost;

        if ("astar".equalsIgnoreCase(algorithm)) {
            AStarAlgorithm.Result result = AStarAlgorithm.findPath(graph, source, destination);
            path = result.path;
            cost = result.cost;
        } else {
            DijkstraAlgorithm.Result result = DijkstraAlgorithm.findShortestPaths(graph, source);
            List<String> reconstructed = DijkstraAlgorithm.reconstructPath(
                result.previous, source, destination);
            path = reconstructed;
            cost = result.distances.getOrDefault(destination, Integer.MAX_VALUE);
        }

        boolean isValid = path != null && !path.isEmpty() && cost != Integer.MAX_VALUE;

        RouteResponse response = new RouteResponse();
        response.setPath(path != null ? path : new ArrayList<>());
        response.setHops(path != null ? path.size() - 1 : 0);
        response.setCost(cost);
        response.setAlgorithm(algorithm);
        response.setIsValid(isValid);

        int baseCost = cost * BASE_COST_PER_KM;
        response.setBaseCost(baseCost);
        response.setDelayPenalty(0);
        response.setTotalCost(baseCost);

        return response;
    }

    public RouteResponse optimizeRoute(String source, String destination) {
        return optimizeRoute(source, destination, "dijkstra");
    }

    public RouteResponse calculateReturnRoute(String customerNode, String warehouseNode) {
        if (warehouseNode == null || warehouseNode.isEmpty()) {
            warehouseNode = "W1";
        }
        return optimizeRoute(customerNode, warehouseNode);
    }

    public RouteResponse calculateCost(RouteResponse route, int delayHours) {
        if (route == null) return null;

        RouteResponse response = new RouteResponse();
        response.setPath(route.getPath());
        response.setHops(route.getHops());
        response.setAlgorithm(route.getAlgorithm());
        response.setIsValid(route.getIsValid());

        int baseCost = route.getCost() * BASE_COST_PER_KM;
        int delayPenalty = delayHours * DELAY_PENALTY_PER_HOUR;
        int total = baseCost + delayPenalty;

        response.setBaseCost(baseCost);
        response.setDelayPenalty(delayPenalty);
        response.setTotalCost(total);

        return response;
    }

    public Map<String, Object> simulateTrafficDelay(String fromNode, String toNode,
                                                   double multiplier) {
        Graph graph = getGraph();
        graph.applyTrafficDelay(fromNode, toNode, multiplier);

        Map<String, Object> result = new HashMap<>();
        result.put("affectedEdge", fromNode + "-" + toNode);
        result.put("multiplier", multiplier);
        result.put("message", "Traffic delay applied on " + fromNode + "→" + toNode +
            " (x" + multiplier + ")");
        return result;
    }

    public void clearAllDelays() {
        getGraph().clearTrafficDelays();
    }

    public Map<String, Double> getActiveDelays() {
        return getGraph().getActiveDelays();
    }

    public Map<String, RouteResponse> getAllRoutesFrom(String source) {
        Graph graph = getGraph();
        DijkstraAlgorithm.Result result = DijkstraAlgorithm.findShortestPaths(graph, source);

        Map<String, RouteResponse> routes = new HashMap<>();
        for (Node node : graph.getAllNodes()) {
            String dest = node.getId();
            if (dest.equals(source)) continue;

            List<String> path = DijkstraAlgorithm.reconstructPath(
                result.previous, source, dest);
            int cost = result.distances.getOrDefault(dest, Integer.MAX_VALUE);

            RouteResponse response = new RouteResponse();
            response.setPath(path);
            response.setHops(path.size() - 1);
            response.setCost(cost);
            response.setIsValid(!path.isEmpty());
            response.setBaseCost(cost * BASE_COST_PER_KM);
            response.setTotalCost(cost * BASE_COST_PER_KM);

            routes.put(dest, response);
        }
        return routes;
    }
}