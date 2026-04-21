package com.supplychain.repository;

import com.supplychain.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Integer> {
    Optional<Inventory> findByProductId(String productId);
    List<Inventory> findByQuantityLessThanEqual(Integer threshold);
    List<Inventory> findByQuantityGreaterThanEqual(Integer threshold);
}