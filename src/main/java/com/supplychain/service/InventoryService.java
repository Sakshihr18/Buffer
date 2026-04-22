package com.supplychain.service;

import com.supplychain.dto.InventoryAlert;
import com.supplychain.model.Inventory;
import com.supplychain.repository.InventoryRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    private static final int LOW_STOCK_THRESHOLD = 10;
    private static final int REORDER_POINT = 15;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    public List<Inventory> findAll() {
        return inventoryRepository.findAll();
    }

    public Optional<Inventory> findByProductId(String productId) {
        return inventoryRepository.findByProductId(productId);
    }

    public Inventory save(Inventory inventory) {
        return inventoryRepository.save(inventory);
    }

    public Inventory createOrUpdate(String productId, Integer quantity, Integer maxCapacity,
                                    Integer reorderPoint, String warehouseId) {
        Optional<Inventory> existing = inventoryRepository.findByProductId(productId);

        if (existing.isPresent()) {
            Inventory inv = existing.get();
            if (quantity != null) inv.setQuantity(quantity);
            if (maxCapacity != null) inv.setMaxCapacity(maxCapacity);
            if (reorderPoint != null) inv.setReorderPoint(reorderPoint);
            if (warehouseId != null) inv.setWarehouseId(warehouseId);
            inv.setLastUpdated(LocalDateTime.now());
            return inventoryRepository.save(inv);
        } else {
            Inventory inv = new Inventory();
            inv.setProductId(productId);
            inv.setQuantity(quantity != null ? quantity : 0);
            inv.setMaxCapacity(maxCapacity != null ? maxCapacity : 200);
            inv.setReorderPoint(reorderPoint != null ? reorderPoint : 15);
            inv.setWarehouseId(warehouseId != null ? warehouseId : "WH-1");
            inv.setReserved(0);
            inv.setLastUpdated(LocalDateTime.now());
            return inventoryRepository.save(inv);
        }
    }

    public Inventory updateStock(String productId, Integer quantity, Integer reserved,
                                Integer maxCapacity, Integer reorderPoint) {
        Inventory inv = inventoryRepository.findByProductId(productId)
            .orElseThrow(() -> new RuntimeException("Inventory not found for product: " + productId));

        if (quantity != null) inv.setQuantity(quantity);
        if (reserved != null) inv.setReserved(reserved);
        if (maxCapacity != null) inv.setMaxCapacity(maxCapacity);
        if (reorderPoint != null) inv.setReorderPoint(reorderPoint);
        inv.setLastUpdated(LocalDateTime.now());

        return inventoryRepository.save(inv);
    }

    public boolean reserveStock(String productId, Integer quantity) {
        Inventory inv = inventoryRepository.findByProductId(productId).orElse(null);
        if (inv == null) return false;

        int available = inv.getQuantity() - inv.getReserved();
        if (available < quantity) return false;

        inv.setReserved(inv.getReserved() + quantity);
        inv.setLastUpdated(LocalDateTime.now());
        inventoryRepository.save(inv);
        return true;
    }

    public void releaseReservation(String productId, Integer quantity) {
        Inventory inv = inventoryRepository.findByProductId(productId).orElse(null);
        if (inv == null) return;

        int newReserved = Math.max(0, inv.getReserved() - quantity);
        int newQuantity = Math.max(0, inv.getQuantity() - quantity);
        inv.setReserved(newReserved);
        inv.setQuantity(newQuantity);
        inv.setLastUpdated(LocalDateTime.now());
        inventoryRepository.save(inv);
    }

    public InventoryAlert getAlertStatus(String productId) {
        Inventory inv = inventoryRepository.findByProductId(productId).orElse(null);
        if (inv == null) return null;

        int q = inv.getQuantity();
        int max = inv.getMaxCapacity();

        if (q == 0) {
            return new InventoryAlert(productId, inv.getProductName(), q, "OUT_OF_STOCK",
                productId + " is OUT OF STOCK!", "critical");
        } else if (q <= LOW_STOCK_THRESHOLD) {
            return new InventoryAlert(productId, inv.getProductName(), q, "LOW_STOCK",
                productId + ": Only " + q + " units left", "warning");
        } else if (q >= max) {
            return new InventoryAlert(productId, inv.getProductName(), q, "OVERSTOCK",
                productId + ": Overstocked (" + q + " units)", "info");
        } else if (q <= REORDER_POINT) {
            return new InventoryAlert(productId, inv.getProductName(), q, "REORDER",
                productId + ": Reorder point reached (" + q + ")", "warning");
        } else {
            return new InventoryAlert(productId, inv.getProductName(), q, "OK",
                productId + ": Stock OK (" + q + ")", "ok");
        }
    }

    public List<InventoryAlert> getAllAlerts() {
        List<InventoryAlert> alerts = new ArrayList<>();
        for (Inventory inv : inventoryRepository.findAll()) {
            InventoryAlert alert = getAlertStatus(inv.getProductId());
            if (alert != null && !"OK".equals(alert.getType())) {
                alerts.add(alert);
            }
        }
        return alerts;
    }

    public Map<String, Object> getStats() {
        List<Inventory> all = inventoryRepository.findAll();
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", all.size());
        long lowStock = 0;
        long outOfStock = 0;
        long overstock = 0;
        for (Inventory i : all) {
            if (i.getQuantity() <= LOW_STOCK_THRESHOLD) lowStock++;
            if (i.getQuantity() == 0) outOfStock++;
            if (i.getQuantity() >= i.getMaxCapacity()) overstock++;
        }
        stats.put("lowStock", lowStock);
        stats.put("outOfStock", outOfStock);
        stats.put("overstock", overstock);
        return stats;
    }
}