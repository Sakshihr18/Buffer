package com.supplychain.config;

import com.supplychain.model.*;
import com.supplychain.model.enums.Priority;
import com.supplychain.repository.*;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class DatabaseInitializer {

    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final InventoryRepository inventoryRepository;
    private final VehicleRepository vehicleRepository;
    private final AgentRepository agentRepository;
    private final OrderRepository orderRepository;

    public DatabaseInitializer(ProductRepository productRepository,
                               CustomerRepository customerRepository,
                               InventoryRepository inventoryRepository,
                               VehicleRepository vehicleRepository,
                               AgentRepository agentRepository,
                               OrderRepository orderRepository) {
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.inventoryRepository = inventoryRepository;
        this.vehicleRepository = vehicleRepository;
        this.agentRepository = agentRepository;
        this.orderRepository = orderRepository;
    }

    @PostConstruct
    public void initialize() {
        if (productRepository.count() == 0) {
            seedProducts();
        }
        if (customerRepository.count() == 0) {
            seedCustomers();
        }
        if (inventoryRepository.count() == 0) {
            seedInventory();
        }
        if (vehicleRepository.count() == 0) {
            seedVehicles();
        }
        if (agentRepository.count() == 0) {
            seedAgents();
        }
        if (orderRepository.count() == 0) {
            seedOrders();
        }
    }

    private void seedProducts() {
        List<Product> products = Arrays.asList(
            createProduct("P001", "Electronics Package", "Electronics", new BigDecimal("500"), new BigDecimal("2.00"), new BigDecimal("0.003")),
            createProduct("P002", "Furniture Set", "Furniture", new BigDecimal("1200"), new BigDecimal("15.00"), new BigDecimal("0.020")),
            createProduct("P003", "Grocery Bundle", "Grocery", new BigDecimal("150"), new BigDecimal("5.00"), new BigDecimal("0.008")),
            createProduct("P004", "Medical Supplies", "Medical", new BigDecimal("800"), new BigDecimal("1.00"), new BigDecimal("0.002")),
            createProduct("P005", "Sports Equipment", "Sports", new BigDecimal("300"), new BigDecimal("8.00"), new BigDecimal("0.010")),
            createProduct("P006", "Clothing Pack", "Apparel", new BigDecimal("200"), new BigDecimal("3.00"), new BigDecimal("0.005")),
            createProduct("P007", "Home Appliances", "Appliances", new BigDecimal("600"), new BigDecimal("12.00"), new BigDecimal("0.015"))
        );
        productRepository.saveAll(products);
    }

    private Product createProduct(String id, String name, String category, BigDecimal price, BigDecimal weight, BigDecimal volume) {
        Product p = new Product();
        p.setId(id);
        p.setName(name);
        p.setCategory(category);
        p.setUnitPrice(price);
        p.setWeightKg(weight);
        p.setVolumeM3(volume);
        p.setCreatedAt(LocalDateTime.now());
        return p;
    }

    private void seedCustomers() {
        List<Customer> customers = Arrays.asList(
            createCustomer("CUST-100", "Arjun Sharma", "arjun@email.com", "9876543210", "12 MG Road, Pune", "N3", 200f, 150f),
            createCustomer("CUST-101", "Priya Patel", "priya@email.com", "9876543211", "45 FC Road, Pune", "N5", 350f, 280f),
            createCustomer("CUST-102", "Rahul Verma", "rahul@email.com", "9876543212", "7 Baner Road, Pune", "N7", 150f, 320f),
            createCustomer("CUST-103", "Sneha Iyer", "sneha@email.com", "9876543213", "88 Koregaon Park", "N4", 400f, 180f),
            createCustomer("CUST-104", "Kiran Mehta", "kiran@email.com", "9876543214", "3 Viman Nagar", "N6", 300f, 380f)
        );
        customerRepository.saveAll(customers);
    }

    private Customer createCustomer(String id, String name, String email, String phone, String address, String zone, float x, float y) {
        Customer c = new Customer();
        c.setId(id);
        c.setName(name);
        c.setEmail(email);
        c.setPhone(phone);
        c.setAddress(address);
        c.setZoneNode(zone);
        c.setXCoord(x);
        c.setYCoord(y);
        c.setCreatedAt(LocalDateTime.now());
        return c;
    }

    private void seedInventory() {
        List<Inventory> items = Arrays.asList(
            createInventory("P001", "WH-1", 45, 5, 100, 15),
            createInventory("P002", "WH-1", 8, 2, 50, 5),
            createInventory("P003", "WH-2", 210, 10, 200, 20),
            createInventory("P004", "WH-2", 3, 1, 80, 10),
            createInventory("P005", "WH-1", 30, 3, 60, 8),
            createInventory("P006", "WH-3", 0, 0, 150, 15),
            createInventory("P007", "WH-1", 12, 2, 40, 5)
        );
        inventoryRepository.saveAll(items);
    }

    private Inventory createInventory(String productId, String warehouseId, int qty, int reserved, int maxCap, int reorder) {
        Inventory i = new Inventory();
        i.setProductId(productId);
        i.setWarehouseId(warehouseId);
        i.setQuantity(qty);
        i.setReserved(reserved);
        i.setMaxCapacity(maxCap);
        i.setReorderPoint(reorder);
        i.setLastUpdated(LocalDateTime.now());
        return i;
    }

    private void seedVehicles() {
        List<Vehicle> vehicles = Arrays.asList(
            createVehicle("V001", "Truck Alpha", "truck", 1000, 500, "W1"),
            createVehicle("V002", "Van Beta", "van", 500, 250, "W1"),
            createVehicle("V003", "Bike Gamma", "bike", 50, 30, "H1"),
            createVehicle("V004", "Truck Delta", "truck", 800, 400, "W2")
        );
        vehicleRepository.saveAll(vehicles);
    }

    private Vehicle createVehicle(String id, String name, String type, int weightCap, int volCap, String node) {
        Vehicle v = new Vehicle();
        v.setId(id);
        v.setName(name);
        v.setType(type);
        v.setWeightCapacity(new BigDecimal(weightCap));
        v.setVolumeCapacity(new BigDecimal(volCap));
        v.setFuelLevel(new BigDecimal(100));
        v.setAvailable(true);
        v.setCurrentNode(node);
        v.setTotalDeliveries(0);
        v.setCreatedAt(LocalDateTime.now());
        return v;
    }

    private void seedAgents() {
        List<Agent> agents = Arrays.asList(
            createAgent("A001", "Ravi Kumar", "9111222333", 120f, 180f, true, new BigDecimal("4.8"), 142),
            createAgent("A002", "Sita Devi", "9111222334", 350f, 280f, true, new BigDecimal("4.6"), 98),
            createAgent("A003", "Mohan Singh", "9111222335", 200f, 150f, false, new BigDecimal("4.9"), 201),
            createAgent("A004", "Lakshmi Nair", "9111222336", 400f, 180f, true, new BigDecimal("4.7"), 77),
            createAgent("A005", "Vijay Reddy", "9111222337", 300f, 380f, true, new BigDecimal("4.5"), 55)
        );
        agentRepository.saveAll(agents);
    }

    private Agent createAgent(String id, String name, String phone, float x, float y, boolean available, BigDecimal rating, int deliveries) {
        Agent a = new Agent();
        a.setId(id);
        a.setName(name);
        a.setPhone(phone);
        a.setXCoord(x);
        a.setYCoord(y);
        a.setAvailable(available);
        a.setRating(rating);
        a.setTotalDeliveries(deliveries);
        a.setCreatedAt(LocalDateTime.now());
        return a;
    }

    private void seedOrders() {
        List<Order> orders = Arrays.asList(
            createOrder("ORD-1001", "CUST-100", "P001", 2, new BigDecimal("4.00"), new BigDecimal("0.006"), new BigDecimal("1000"), Priority.HIGH, "N3", 1),
            createOrder("ORD-1002", "CUST-101", "P002", 1, new BigDecimal("15.00"), new BigDecimal("0.020"), new BigDecimal("1200"), Priority.MEDIUM, "N5", 2),
            createOrder("ORD-1003", "CUST-102", "P003", 3, new BigDecimal("15.00"), new BigDecimal("0.024"), new BigDecimal("450"), Priority.LOW, "N7", 3),
            createOrder("ORD-1004", "CUST-103", "P004", 1, new BigDecimal("1.00"), new BigDecimal("0.002"), new BigDecimal("800"), Priority.HIGH, "N4", 1),
            createOrder("ORD-1005", "CUST-104", "P005", 2, new BigDecimal("16.00"), new BigDecimal("0.020"), new BigDecimal("600"), Priority.MEDIUM, "N6", 2)
        );
        orderRepository.saveAll(orders);
    }

    private Order createOrder(String id, String custId, String prodId, int qty, BigDecimal weight, BigDecimal volume, BigDecimal value, Priority priority, String destNode, int days) {
        Order o = new Order();
        o.setId(id);
        o.setCustomerId(custId);
        o.setProductId(prodId);
        o.setQuantity(qty);
        o.setTotalWeight(weight);
        o.setTotalVolume(volume);
        o.setTotalValue(value);
        o.setPriority(priority);
        o.setStatus(com.supplychain.model.enums.OrderStatus.PENDING);
        o.setDestinationNode(destNode);
        o.setDeadline(LocalDateTime.now().plusDays(days));
        o.setCreatedAt(LocalDateTime.now());
        o.setUpdatedAt(LocalDateTime.now());
        return o;
    }
}