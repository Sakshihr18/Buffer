package com.supplychain.service;

import com.supplychain.model.Order;
import com.supplychain.model.enums.OrderStatus;
import com.supplychain.model.enums.Priority;
import com.supplychain.repository.OrderRepository;
import com.supplychain.repository.ProductRepository;
import com.supplychain.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final AtomicInteger idCounter = new AtomicInteger(1000);

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository,
                      CustomerRepository customerRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
    }

    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    public Optional<Order> findById(String id) {
        return orderRepository.findById(id);
    }

    public List<Order> findByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    public Order save(Order order) {
        return orderRepository.save(order);
    }

    public Order createOrder(String customerId, String productId, Integer quantity,
                            BigDecimal weight, BigDecimal volume, BigDecimal value,
                            Priority priority, String destinationNode, Integer daysToDeadline,
                            String notes) {
        String orderId = "ORD-" + idCounter.incrementAndGet();

        Order order = new Order();
        order.setId(orderId);
        order.setCustomerId(customerId);
        order.setProductId(productId);
        order.setQuantity(quantity);
        order.setTotalWeight(weight);
        order.setTotalVolume(volume);
        order.setTotalValue(value);
        order.setPriority(priority != null ? priority : Priority.MEDIUM);
        order.setStatus(OrderStatus.PENDING);
        order.setDestinationNode(destinationNode);
        order.setNotes(notes);

        if (daysToDeadline != null && daysToDeadline > 0) {
            order.setDeadline(LocalDateTime.now().plusDays(daysToDeadline));
        } else {
            order.setDeadline(LocalDateTime.now().plusDays(3));
        }

        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        return orderRepository.save(order);
    }

    public Order updateOrder(String id, Order updated) {
        Order existing = orderRepository.findById(id).orElseThrow();
        existing.setCustomerId(updated.getCustomerId());
        existing.setProductId(updated.getProductId());
        existing.setQuantity(updated.getQuantity());
        existing.setTotalWeight(updated.getTotalWeight());
        existing.setTotalVolume(updated.getTotalVolume());
        existing.setTotalValue(updated.getTotalValue());
        existing.setPriority(updated.getPriority());
        existing.setStatus(updated.getStatus());
        existing.setDestinationNode(updated.getDestinationNode());
        existing.setDeadline(updated.getDeadline());
        existing.setAssignedVehicle(updated.getAssignedVehicle());
        existing.setAssignedAgent(updated.getAssignedAgent());
        existing.setDeliveryCost(updated.getDeliveryCost());
        existing.setFailReason(updated.getFailReason());
        existing.setNotes(updated.getNotes());
        existing.setUpdatedAt(LocalDateTime.now());
        return orderRepository.save(existing);
    }

    public Order updateStatus(String id, OrderStatus status) {
        Order order = orderRepository.findById(id).orElseThrow();
        order.setStatus(status);
        order.setUpdatedAt(LocalDateTime.now());
        return orderRepository.save(order);
    }

    public void delete(String id) {
        orderRepository.deleteById(id);
    }

    public Map<String, Long> getStats() {
        Map<String, Long> stats = new HashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            stats.put(status.name(), orderRepository.findByStatus(status).stream().count());
        }
        stats.put("total", orderRepository.count());
        return stats;
    }
}