import java.util.*;

public class index {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        Graph graph = new Graph();
        InventoryManager inventory = new InventoryManager();
        OrderManager orderManager = new OrderManager();
        RoutingEngine routing = new RoutingEngine(graph);
        FailureManager failure = new FailureManager(graph);
        Rebalancer rebalancer = new Rebalancer(graph, inventory);
        BottleneckDetector bottleneck = new BottleneckDetector(graph);

        while (true) {

            System.out.println("\n===== SUPPLY CHAIN OPTIMIZER =====");
            System.out.println("1. Add Warehouse");
            System.out.println("2. Add Route");
            System.out.println("3. Add Inventory");
            System.out.println("4. Create Order");
            System.out.println("5. Process Order");
            System.out.println("6. Simulate Failure");
            System.out.println("7. Show Bottlenecks");
            System.out.println("8. Exit");

            int choice = sc.nextInt();

            switch (choice) {

                case 1:
                    System.out.print("Enter Warehouse ID: ");
                    String wid = sc.next();
                    graph.addWarehouse(wid);
                    break;

                case 2:
                    System.out.print("From: ");
                    String u = sc.next();
                    System.out.print("To: ");
                    String v = sc.next();
                    System.out.print("Distance: ");
                    int w = sc.nextInt();
                    graph.addRoute(u, v, w);
                    break;

                case 3:
                    System.out.print("Warehouse: ");
                    String w1 = sc.next();
                    System.out.print("Product: ");
                    String p = sc.next();
                    System.out.print("Quantity: ");
                    int q = sc.nextInt();
                    inventory.addStock(w1, p, q);
                    break;

                case 4:
                    System.out.print("Order ID: ");
                    String oid = sc.next();
                    System.out.print("Source: ");
                    String s = sc.next();
                    System.out.print("Destination: ");
                    String d = sc.next();
                    System.out.print("Product: ");
                    String prod = sc.next();
                    System.out.print("Quantity: ");
                    int qty = sc.nextInt();
                    System.out.print("Priority (1=High): ");
                    int pr = sc.nextInt();

                    orderManager.addOrder(new Order(oid, s, d, prod, qty, pr));
                    break;

                case 5:
                    Order order = orderManager.getNextOrder();

                    if (order == null) {
                        System.out.println("No Orders Available");
                        break;
                    }

                    System.out.println("\nProcessing Order: " + order.id);

                    if (!inventory.checkStock(order.source, order.product, order.quantity)) {
                        System.out.println("⚠️ Insufficient stock → Rebalancing");
                        rebalancer.rebalance(order.product);
                        break;
                    }

                    if (graph.isWarehouseFull(order.destination)) {
                        System.out.println("⚠️ Destination full → Switching warehouse");
                        order.destination = graph.findAlternateWarehouse(order.destination);
                    }

                    List<String> path = routing.getRoute(order.source, order.destination);
                    System.out.println("🚚 Route: " + path);

                    inventory.updateStock(order.source, order.product, -order.quantity);
                    graph.updateLoad(order.destination, order.quantity);

                    break;

                case 6:
                    System.out.print("Enter failed warehouse: ");
                    String fail = sc.next();
                    failure.handleFailure(fail);
                    System.out.println("🚨 Failure handled.");
                    break;

                case 7:
                    bottleneck.detect();
                    break;

                case 8:
                    System.exit(0);
            }
        }
    }
}

class Graph {

    Map<String, Warehouse> warehouses = new HashMap<>();
    Map<String, List<Edge>> adj = new HashMap<>();

    void addWarehouse(String id) {
        warehouses.put(id, new Warehouse(id));
        adj.put(id, new ArrayList<>());
    }

    void addRoute(String u, String v, int w) {
        adj.get(u).add(new Edge(v, w));
        adj.get(v).add(new Edge(u, w));
    }

    void removeWarehouse(String id) {
        adj.remove(id);
        warehouses.remove(id);

        for (List<Edge> list : adj.values()) {
            list.removeIf(e -> e.to.equals(id));
        }
    }

    boolean isWarehouseFull(String id) {
        return warehouses.get(id).isFull();
    }

    void updateLoad(String id, int qty) {
        warehouses.get(id).currentLoad += qty;
    }

    String findAlternateWarehouse(String failed) {
        for (String w : warehouses.keySet()) {
            if (!warehouses.get(w).isFull()) return w;
        }
        return failed;
    }
}

/////////////////////////////////////////////////////////////////
// ========================= WAREHOUSE =========================
class Warehouse {
    String id;
    int capacity = 1000;
    int currentLoad = 0;

    Warehouse(String id) {
        this.id = id;
    }

    boolean isFull() {
        return currentLoad >= capacity;
    }
}


class Edge {
    String to;
    int weight;

    Edge(String t, int w) {
        to = t;
        weight = w;
    }
}


class Order {
    String id, source, destination, product;
    int quantity, priority;

    Order(String i, String s, String d, String p, int q, int pr) {
        id = i;
        source = s;
        destination = d;
        product = p;
        quantity = q;
        priority = pr;
    }
}

class OrderManager {
    PriorityQueue<Order> pq = new PriorityQueue<>((a, b) -> a.priority - b.priority);

    void addOrder(Order o) {
        pq.add(o);
    }

    Order getNextOrder() {
        return pq.poll();
    }
}

class RoutingEngine {

    Graph graph;

    RoutingEngine(Graph g) {
        graph = g;
    }

    List<String> getRoute(String src, String dest) {

        Map<String, Integer> dist = new HashMap<>();
        Map<String, String> parent = new HashMap<>();
        PriorityQueue<Pair> pq = new PriorityQueue<>((a, b) -> a.dist - b.dist);

        for (String node : graph.adj.keySet()) {
            dist.put(node, Integer.MAX_VALUE);
        }

        dist.put(src, 0);
        pq.add(new Pair(src, 0));

        while (!pq.isEmpty()) {
            Pair curr = pq.poll();

            for (Edge e : graph.adj.get(curr.node)) {
                int newDist = curr.dist + e.weight;

                if (newDist < dist.get(e.to)) {
                    dist.put(e.to, newDist);
                    parent.put(e.to, curr.node);
                    pq.add(new Pair(e.to, newDist));
                }
            }
        }

        List<String> path = new ArrayList<>();
        String curr = dest;

        while (curr != null) {
            path.add(curr);
            curr = parent.get(curr);
        }

        Collections.reverse(path);
        return path;
    }

    static class Pair {
        String node;
        int dist;

        Pair(String n, int d) {
            node = n;
            dist = d;
        }
    }
}


class InventoryManager {

    Map<String, Map<String, Integer>> stock = new HashMap<>();

    void addStock(String w, String p, int qty) {
        stock.putIfAbsent(w, new HashMap<>());
        stock.get(w).put(p, stock.get(w).getOrDefault(p, 0) + qty);
    }

    boolean checkStock(String w, String p, int qty) {
        return stock.containsKey(w) &&
                stock.get(w).getOrDefault(p, 0) >= qty;
    }

    void updateStock(String w, String p, int change) {
        stock.get(w).put(p,
                stock.get(w).get(p) + change);
    }
}


class FailureManager {

    Graph graph;

    FailureManager(Graph g) {
        graph = g;
    }

    void handleFailure(String w) {
        graph.removeWarehouse(w);
    }
}


class Rebalancer {

    Graph graph;
    InventoryManager inventory;

    Rebalancer(Graph g, InventoryManager i) {
        graph = g;
        inventory = i;
    }

    void rebalance(String product) {
        System.out.println("🔁 Rebalancing for " + product);

        for (String w : inventory.stock.keySet()) {
            int qty = inventory.stock.get(w).getOrDefault(product, 0);

            if (qty > 100) {
                System.out.println("Surplus at: " + w);
            }
        }
    }
}


class BottleneckDetector {

    Graph graph;

    BottleneckDetector(Graph g) {
        graph = g;
    }

    void detect() {
        PriorityQueue<Warehouse> pq =
                new PriorityQueue<>((a, b) -> b.currentLoad - a.currentLoad);

        pq.addAll(graph.warehouses.values());

        System.out.println("\n📊 Bottleneck Analysis:");
        while (!pq.isEmpty()) {
            Warehouse w = pq.poll();
            System.out.println(w.id + " Load: " + w.currentLoad);
        }
    }
}