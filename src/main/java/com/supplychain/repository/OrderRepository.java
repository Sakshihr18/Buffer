package com.supplychain.repository;

import com.supplychain.model.Order;
import com.supplychain.model.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    List<Order> findByStatus(OrderStatus status);
    List<Order> findByCustomerId(String customerId);
}