package com.supplychain.service;

import org.springframework.stereotype.Service;
import jakarta.persistence.*;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
public class ReplenishmentService {

    @PersistenceContext
    private EntityManager em;

    public static class ReorderItem {
        public String productId;
        public String productName;
        public int currentStock;
        public int reorderPoint;
        public int reorderQty;
        public int daysUntilStockout;
        public String urgency;
        public String recommendation;
        public LocalDate estimatedReorderDate;

        public ReorderItem(String productId, String productName, int currentStock,
                      int reorderPoint, int reorderQty, int daysUntilStockout,
                      String urgency, String recommendation, LocalDate estimatedReorderDate) {
            this.productId = productId;
            this.productName = productName;
            this.currentStock = currentStock;
            this.reorderPoint = reorderPoint;
            this.reorderQty = reorderQty;
            this.daysUntilStockout = daysUntilStockout;
            this.urgency = urgency;
            this.recommendation = recommendation;
            this.estimatedReorderDate = estimatedReorderDate;
        }
    }

    public static class DemandForecast {
        public String productId;
        public double avgDailyDemand;
        public double peakHourDemand;
        public int predictedStockoutDays;
        public int recommendedReorderQty;

        public DemandForecast(String productId, double avgDailyDemand, double peakHourDemand,
                         int predictedStockoutDays, int recommendedReorderQty) {
            this.productId = productId;
            this.avgDailyDemand = avgDailyDemand;
            this.peakHourDemand = peakHourDemand;
            this.predictedStockoutDays = predictedStockoutDays;
            this.recommendedReorderQty = recommendedReorderQty;
        }
    }

    @Transactional
    public List<ReorderItem> getReorderQueue(String warehouseId) {
        List<ReorderItem> queue = new ArrayList<>();

        Query query = em.createNativeQuery(
            "SELECT r.product_id, p.name, " +
            "COALESCE(SUM(i.quantity - i.reserved_qty), 0) as current_stock, " +
            "r.reorder_point, r.reorder_qty, r.lead_time_hours, r.safety_days " +
            "FROM reorder_rules r " +
            "JOIN products p ON r.product_id = p.id " +
            "LEFT JOIN inventory_batches i ON r.product_id = i.product_id AND i.warehouse_id = r.warehouse_id AND i.status = 'AVAILABLE' " +
            "WHERE r.warehouse_id = ?1 AND r.is_active = TRUE " +
            "GROUP BY r.product_id, p.name, r.reorder_point, r.reorder_qty, r.lead_time_hours, r.safety_days " +
            "HAVING current_stock < r.reorder_point " +
            "ORDER BY " +
            "CASE WHEN current_stock < r.reorder_point * 0.5 THEN 1 " +
            "WHEN current_stock < r.reorder_point THEN 2 " +
            "ELSE 3 END, current_stock ASC"
        );
        query.setParameter(1, warehouseId);

        LocalDate today = LocalDate.now();
        @SuppressWarnings("unchecked")
        List<Object[]> reorderList = (List<Object[]>) query.getResultList();
        for (Object[] row : reorderList) {
            String productId = (String) row[0];
            String productName = (String) row[1];
            int currentStock = ((Number) row[2]).intValue();
            int reorderPoint = ((Number) row[3]).intValue();
            int reorderQty = ((Number) row[4]).intValue();
            int safetyDays = ((Number) row[6]).intValue();

            DemandForecast forecast = predictStockout(productId, warehouseId);
            int daysUntilStockout = forecast != null ? forecast.predictedStockoutDays : 999;

            String urgency;
            String recommendation;
            LocalDate estReorderDate;

            if (currentStock <= 0) {
                urgency = "CRITICAL";
                recommendation = "EMERGENCY - Out of stock! Order immediately.";
                estReorderDate = today;
            } else if (currentStock < reorderPoint * 0.5) {
                urgency = "HIGH";
                recommendation = "Stock below 50% - Rush reorder required.";
                estReorderDate = today.plusDays(1);
            } else if (currentStock < reorderPoint) {
                urgency = "MEDIUM";
                recommendation = "Reorder point reached - Initiate standard reorder.";
                estReorderDate = today.plusDays(1);
            } else {
                urgency = "LOW";
                recommendation = "Monitor - Stock running low.";
                estReorderDate = today.plusDays(daysUntilStockout - safetyDays);
            }

            queue.add(new ReorderItem(
                productId, productName, currentStock,
                reorderPoint, reorderQty, daysUntilStockout,
                urgency, recommendation, estReorderDate
            ));
        }

        return queue;
    }

    public DemandForecast predictStockout(String productId, String warehouseId) {
        Query demandQuery = em.createNativeQuery(
            "SELECT AVG(quantity_sold) as avg_demand, MAX(quantity_sold) as peak " +
            "FROM demand_history " +
            "WHERE product_id = ?1 AND warehouse_id = ?2 " +
            "AND date >= CURRENT_DATE - INTERVAL '7' DAY"
        );
        demandQuery.setParameter(1, productId);
        demandQuery.setParameter(2, warehouseId);

        Object[] result = (Object[]) demandQuery.getSingleResult();
        double avgDemand = result[0] != null ? ((Number) result[0]).doubleValue() : 10.0;
        double peakDemand = result[1] != null ? ((Number) result[1]).doubleValue() : avgDemand * 1.5;

        Query stockQuery = em.createNativeQuery(
            "SELECT COALESCE(SUM(quantity - reserved_qty), 0) " +
            "FROM inventory_batches " +
            "WHERE product_id = ?1 AND warehouse_id = ?2 AND status = 'AVAILABLE'"
        );
        stockQuery.setParameter(1, productId);
        stockQuery.setParameter(2, warehouseId);
        int currentStock = ((Number) stockQuery.getSingleResult()).intValue();

        int daysUntilStockout = avgDemand > 0 ? (int) (currentStock / avgDemand) : 999;
        int recommendedOrderQty = (int) (avgDemand * 14) + (int) (peakDemand * 2);

        return new DemandForecast(productId, avgDemand, peakDemand, daysUntilStockout, recommendedOrderQty);
    }

    public Map<String, Object> getReplenishmentStats(String warehouseId) {
        Map<String, Object> stats = new HashMap<>();

        Query totalProducts = em.createNativeQuery(
            "SELECT COUNT(*) FROM reorder_rules WHERE warehouse_id = ?1 AND is_active = TRUE"
        );
        totalProducts.setParameter(1, warehouseId);
        stats.put("totalConfigured", ((Number) totalProducts.getSingleResult()).intValue());

        Query needReorder = em.createNativeQuery(
            "SELECT COUNT(DISTINCT r.product_id) " +
            "FROM reorder_rules r " +
            "LEFT JOIN inventory_batches i ON r.product_id = i.product_id AND i.warehouse_id = r.warehouse_id " +
            "WHERE r.warehouse_id = ?1 AND r.is_active = TRUE " +
            "AND COALESCE(SUM(i.quantity - i.reserved_qty), 0) < r.reorder_point " +
            "GROUP BY r.product_id"
        );
        needReorder.setParameter(1, warehouseId);
        @SuppressWarnings("unchecked")
        List<Object> result = needReorder.getResultList();
        stats.put("needReorder", result.size());

        Query critical = em.createNativeQuery(
            "SELECT COUNT(*) FROM reorder_rules r " +
            "JOIN (SELECT product_id, SUM(quantity - reserved_qty) as stock " +
            "     FROM inventory_batches WHERE warehouse_id = ?1 GROUP BY product_id) i " +
            "ON r.product_id = i.product_id " +
            "WHERE r.warehouse_id = ?1 AND r.is_active = TRUE AND i.stock < r.reorder_point * 0.5"
        );
        critical.setParameter(1, warehouseId);
        stats.put("criticalStock", ((Number) critical.getSingleResult()).intValue());

        Query avgDemand = em.createNativeQuery(
            "SELECT AVG(quantity_sold) FROM demand_history " +
            "WHERE warehouse_id = ?1 AND date >= CURRENT_DATE - INTERVAL '7' DAY"
        );
        avgDemand.setParameter(1, warehouseId);
        Number avg = (Number) avgDemand.getSingleResult();
        stats.put("avgDailyDemand", avg != null ? avg.doubleValue() : 0);

        return stats;
    }

    @Transactional
    public boolean updateReorderRule(String productId, String warehouseId, int reorderPoint, int reorderQty, int leadTimeHours) {
        int updated = em.createNativeQuery(
            "UPDATE reorder_rules SET reorder_point = ?1, reorder_qty = ?2, lead_time_hours = ?3 " +
            "WHERE product_id = ?4 AND warehouse_id = ?5"
        )
        .setParameter(1, reorderPoint)
        .setParameter(2, reorderQty)
        .setParameter(3, leadTimeHours)
        .setParameter(4, productId)
        .setParameter(5, warehouseId)
        .executeUpdate();

        return updated > 0;
    }
}