package com.supplychain.util;

import java.util.*;
import java.util.stream.Collectors;

public class KDTreeEngine {

    public static class Point {
        private final String id;
        private final double x;
        private final double y;
        private Object data;

        public Point(String id, double x, double y) {
            this.id = id;
            this.x = x;
            this.y = y;
        }

        public Point(String id, double x, double y, Object data) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.data = data;
        }

        public String getId() { return id; }
        public double getX() { return x; }
        public double getY() { return y; }
        public Object getData() { return data; }
        public void setData(Object data) { this.data = data; }

        public double distanceTo(Point other) {
            double dx = this.x - other.x;
            double dy = this.y - other.y;
            return Math.sqrt(dx * dx + dy * dy);
        }

        @Override
        public String toString() {
            return String.format("Point(%s, %.2f, %.2f)", id, x, y);
        }
    }

    public static class Node {
        private final Point point;
        private Node left;
        private Node right;
        private final int depth;

        public Node(Point point, int depth) {
            this.point = point;
            this.left = null;
            this.right = null;
            this.depth = depth;
        }

        public Point getPoint() { return point; }
        public Node getLeft() { return left; }
        public Node getRight() { return right; }
        public void setLeft(Node left) { this.left = left; }
        public void setRight(Node right) { this.right = right; }
        public int getDepth() { return depth; }
    }

    public static class NearestResult {
        private final String agentId;
        private final double distance;
        private final double x;
        private final double y;
        private final Object agentData;

        public NearestResult(String agentId, double distance, double x, double y, Object agentData) {
            this.agentId = agentId;
            this.distance = distance;
            this.x = x;
            this.y = y;
            this.agentData = agentData;
        }

        public String getAgentId() { return agentId; }
        public double getDistance() { return distance; }
        public double getX() { return x; }
        public double getY() { return y; }
        public Object getAgentData() { return agentData; }
    }

    public static class RouteInsertionResult {
        private final String orderId;
        private final String riderId;
        private final int insertionIndex;
        private final double additionalDistance;
        private final double newTotalDistance;
        private final boolean isOptimal;
        private final String reasoning;

        public RouteInsertionResult(String orderId, String riderId, int insertionIndex,
                                  double additionalDistance, double newTotalDistance,
                                  boolean isOptimal, String reasoning) {
            this.orderId = orderId;
            this.riderId = riderId;
            this.insertionIndex = insertionIndex;
            this.additionalDistance = additionalDistance;
            this.newTotalDistance = newTotalDistance;
            this.isOptimal = isOptimal;
            this.reasoning = reasoning;
        }

        public String getOrderId() { return orderId; }
        public String getRiderId() { return riderId; }
        public int getInsertionIndex() { return insertionIndex; }
        public double getAdditionalDistance() { return additionalDistance; }
        public double getNewTotalDistance() { return newTotalDistance; }
        public boolean isOptimal() { return isOptimal; }
        public String getReasoning() { return reasoning; }
    }

    public static class BatchAssignmentResult {
        private final Map<String, List<String>> orderToRider;
        private final Map<String, Double> riderDistances;
        private final double totalDistance;
        private final int successfullyAssigned;
        private final List<String> unassignedOrders;

        public BatchAssignmentResult(Map<String, List<String>> orderToRider,
                                    Map<String, Double> riderDistances,
                                    double totalDistance,
                                    int successfullyAssigned,
                                    List<String> unassignedOrders) {
            this.orderToRider = orderToRider;
            this.riderDistances = riderDistances;
            this.totalDistance = totalDistance;
            this.successfullyAssigned = successfullyAssigned;
            this.unassignedOrders = unassignedOrders;
        }

        public Map<String, List<String>> getOrderToRider() { return orderToRider; }
        public Map<String, Double> getRiderDistances() { return riderDistances; }
        public double getTotalDistance() { return totalDistance; }
        public int getSuccessfullyAssigned() { return successfullyAssigned; }
        public List<String> getUnassignedOrders() { return unassignedOrders; }
    }

    private Node root;
    private int size;
    private final int dimensions = 2;

    public KDTreeEngine() {
        this.root = null;
        this.size = 0;
    }

    public void insert(Point point) {
        root = insertRecursive(root, point, 0);
        size++;
    }

    private Node insertRecursive(Node node, Point point, int depth) {
        if (node == null) {
            return new Node(point, depth);
        }

        int axis = depth % dimensions;
        if (axis == 0) {
            if (point.getX() < node.getPoint().getX()) {
                node.setLeft(insertRecursive(node.getLeft(), point, depth + 1));
            } else {
                node.setRight(insertRecursive(node.getRight(), point, depth + 1));
            }
        } else {
            if (point.getY() < node.getPoint().getY()) {
                node.setLeft(insertRecursive(node.getLeft(), point, depth + 1));
            } else {
                node.setRight(insertRecursive(node.getRight(), point, depth + 1));
            }
        }

        return node;
    }

    public NearestResult nearestNeighbor(Point target) {
        if (root == null) return null;

        Node[] best = {null};
        double[] bestDistance = {Double.MAX_VALUE};

        nearestNeighborRecursive(root, target, 0, best, bestDistance);

        if (best[0] == null) return null;

        return new NearestResult(
            best[0].getPoint().getId(),
            bestDistance[0],
            best[0].getPoint().getX(),
            best[0].getPoint().getY(),
            best[0].getPoint().getData()
        );
    }

    private void nearestNeighborRecursive(Node node, Point target, int depth,
                                     Node[] best, double[] bestDistance) {
        if (node == null) return;

        double dist = node.getPoint().distanceTo(target);
        if (dist < bestDistance[0]) {
            best[0] = node;
            bestDistance[0] = dist;
        }

        int axis = depth % dimensions;
        boolean goLeft = axis == 0 ?
            target.getX() < node.getPoint().getX() :
            target.getY() < node.getPoint().getY();

        Node primary = goLeft ? node.getLeft() : node.getRight();
        Node secondary = goLeft ? node.getRight() : node.getLeft();

        nearestNeighborRecursive(primary, target, depth + 1, best, bestDistance);

        double axisDist = axis == 0 ?
            Math.abs(target.getX() - node.getPoint().getX()) :
            Math.abs(target.getY() - node.getPoint().getY());

        if (axisDist < bestDistance[0]) {
            nearestNeighborRecursive(secondary, target, depth + 1, best, bestDistance);
        }
    }

    public List<NearestResult> kNearestNeighbors(Point target, int k) {
        List<Node> candidates = new ArrayList<>();
        collectAllNodes(root, candidates);

        if (candidates.isEmpty()) return new ArrayList<>();

        PriorityQueue<Node> pq = new PriorityQueue<>(
            k, (a, b) -> {
                double distA = a.getPoint().distanceTo(target);
                double distB = b.getPoint().distanceTo(target);
                return Double.compare(distA, distB);
            }
        );

        for (Node node : candidates) {
            pq.add(node);
            if (pq.size() > k) pq.poll();
        }

        List<NearestResult> results = new ArrayList<>();
        while (!pq.isEmpty()) {
            Node node = pq.poll();
            Point p = node.getPoint();
            results.add(0, new NearestResult(p.getId(), p.distanceTo(target), p.getX(), p.getY(), p.getData()));
        }

        return results;
    }

    private void collectAllNodes(Node node, List<Node> nodes) {
        if (node == null) return;
        nodes.add(node);
        collectAllNodes(node.getLeft(), nodes);
        collectAllNodes(node.getRight(), nodes);
    }

    public List<NearestResult> findNearestRiders(Point target, int count) {
        return kNearestNeighbors(target, count);
    }

    public RouteInsertionResult findBestInsertion(Point riderLocation, List<Point> currentRoute,
                                          Point newDelivery) {
        if (currentRoute == null || currentRoute.isEmpty()) {
            double dist = riderLocation.distanceTo(newDelivery);
            return new RouteInsertionResult(
                newDelivery.getId(),
                riderLocation.getId(),
                0,
                dist,
                dist,
                true,
                "First delivery in route"
            );
        }

        int bestIndex = -1;
        double bestAdditionalDistance = Double.MAX_VALUE;
        double bestTotalDistance = Double.MAX_VALUE;

        for (int i = 0; i <= currentRoute.size(); i++) {
            double totalDist = calculateInsertionCost(riderLocation, currentRoute, newDelivery, i);
            double additionalDist = totalDist - calculateRouteDistance(currentRoute);

            if (additionalDist < bestAdditionalDistance) {
                bestAdditionalDistance = additionalDist;
                bestTotalDistance = totalDist;
                bestIndex = i;
            }
        }

        boolean isOptimal = bestAdditionalDistance < riderLocation.distanceTo(newDelivery);

        return new RouteInsertionResult(
            newDelivery.getId(),
            riderLocation.getId(),
            bestIndex,
            bestAdditionalDistance,
            bestTotalDistance,
            isOptimal,
            String.format("Insert at position %d, +%.2f distance", bestIndex, bestAdditionalDistance)
        );
    }

    private double calculateRouteDistance(List<Point> route) {
        if (route.isEmpty()) return 0;

        double total = 0;
        for (int i = 0; i < route.size() - 1; i++) {
            total += route.get(i).distanceTo(route.get(i + 1));
        }
        return total;
    }

    private double calculateInsertionCost(Point start, List<Point> route,
                                           Point newPoint, int insertIndex) {
        double total = 0;

        Point previous = start;
        for (int i = 0; i < route.size(); i++) {
            if (i == insertIndex) {
                total += previous.distanceTo(newPoint);
                previous = newPoint;
            }
            total += previous.distanceTo(route.get(i));
            previous = route.get(i);
        }

        if (insertIndex == route.size()) {
            total += previous.distanceTo(newPoint);
        }

        return total;
    }

    public BatchAssignmentResult assignOrdersToRiders(Map<String, Point> riderLocations,
                                                     Map<String, Point> orderLocations,
                                                     int maxOrdersPerRider) {
        Map<String, List<String>> orderToRider = new HashMap<>();
        Map<String, Double> riderDistances = new HashMap<>();
        List<String> unassigned = new ArrayList<>();

        Map<String, Point> availableRiders = new HashMap<>(riderLocations);

        for (Map.Entry<String, Point> entry : orderLocations.entrySet()) {
            String orderId = entry.getKey();
            Point orderLoc = entry.getValue();

            NearestResult nearest = null;
            for (Map.Entry<String, Point> rider : availableRiders.entrySet()) {
                if (orderToRider.containsKey(rider.getKey()) &&
                    orderToRider.get(rider.getKey()).size() >= maxOrdersPerRider) {
                    continue;
                }

                Point riderPoint = rider.getValue();
                double dist = riderPoint.distanceTo(orderLoc);

                if (nearest == null || dist < nearest.getDistance()) {
                    nearest = new NearestResult(rider.getKey(), dist,
                        riderPoint.getX(), riderPoint.getY(), null);
                }
            }

            if (nearest != null) {
                orderToRider.computeIfAbsent(nearest.getAgentId(), k -> new ArrayList<>())
                    .add(orderId);

                double currentDist = riderDistances.getOrDefault(nearest.getAgentId(), 0.0);
                riderDistances.put(nearest.getAgentId(), currentDist + nearest.getDistance());
            } else {
                unassigned.add(orderId);
            }
        }

        double totalDistance = riderDistances.values().stream()
            .mapToDouble(Double::doubleValue).sum();

        return new BatchAssignmentResult(
            orderToRider, riderDistances, totalDistance,
            orderToRider.size(), unassigned
        );
    }

    public List<Point> getAllPoints() {
        List<Node> nodes = new ArrayList<>();
        collectAllNodes(root, nodes);
        return nodes.stream()
            .map(n -> n.getPoint())
            .collect(Collectors.toList());
    }

    public int getSize() {
        return size;
    }

    public void clear() {
        root = null;
        size = 0;
    }

    public static double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) +
                  Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                  Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}