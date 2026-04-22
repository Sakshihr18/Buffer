package com.supplychain.service;

import org.springframework.stereotype.Service;
import jakarta.persistence.*;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class FEFOService {

    @PersistenceContext
    private EntityManager em;

    public static class BatchResult {
        public String batchId;
        public String productId;
        public String productName;
        public int quantity;
        public LocalDate expiryDate;
        public long daysUntilExpiry;
        public String status;
        public String recommendation;

        public BatchResult(String batchId, String productId, String productName, int quantity,
                      LocalDate expiryDate, long daysUntilExpiry, String status, String recommendation) {
            this.batchId = batchId;
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
            this.expiryDate = expiryDate;
            this.daysUntilExpiry = daysUntilExpiry;
            this.status = status;
            this.recommendation = recommendation;
        }
    }

    public static class PickingResult {
        public boolean success;
        public String message;
        public List<BatchResult> batches;
        public int totalQuantity;
        public List<String> warnings;

        public PickingResult(boolean success, String message, List<BatchResult> batches, int totalQuantity) {
            this.success = success;
            this.message = message;
            this.batches = batches;
            this.totalQuantity = totalQuantity;
            this.warnings = new ArrayList<>();
        }
    }

    public static class ExpiryAlert {
        public String batchId;
        public String productId;
        public String productName;
        public int quantity;
        public LocalDate expiryDate;
        public long hoursRemaining;
        public String alertLevel;

        public ExpiryAlert(String batchId, String productId, String productName, int quantity,
                        LocalDate expiryDate, long hoursRemaining, String alertLevel) {
            this.batchId = batchId;
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
            this.expiryDate = expiryDate;
            this.hoursRemaining = hoursRemaining;
            this.alertLevel = alertLevel;
        }
    }

    @Transactional
    public PickingResult getPickingRecommendation(String productId, int requestedQty, String warehouseId) {
        Query batchQuery = em.createNativeQuery(
            "SELECT b.id, b.product_id, p.name, b.quantity, b.expiry_date, b.status " +
            "FROM inventory_batches b JOIN products p ON b.product_id = p.id " +
            "WHERE b.product_id = ?1 AND b.warehouse_id = ?2 " +
            "AND b.status = 'AVAILABLE' AND b.quantity > b.reserved_qty " +
            "ORDER BY b.expiry_date ASC"
        );
        batchQuery.setParameter(1, productId);
        batchQuery.setParameter(2, warehouseId);

        @SuppressWarnings("unchecked")
        List<Object[]> batches = (List<Object[]>) batchQuery.getResultList();
        List<BatchResult> picks = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        int remaining = requestedQty;
        int totalAllocated = 0;

        for (Object[] row : batches) {
            if (remaining <= 0) break;

            int batchId = ((Number) row[0]).intValue();
            String pid = (String) row[1];
            String pName = (String) row[2];
            int qty = ((Number) row[3]).intValue();
            LocalDate expDate = ((java.sql.Date) row[4]).toLocalDate();

            long hoursLeft = ChronoUnit.HOURS.between(Instant.now(), expDate.atStartOfDay());

            if (hoursLeft < 0) continue;

            int qtyToPick = Math.min(qty, remaining);

            BatchResult pick = new BatchResult(
                "BATCH-" + batchId,
                pid, pName, qtyToPick, expDate, hoursLeft / 24,
                getBatchStatus(hoursLeft),
                getRecommendation(hoursLeft)
            );
            picks.add(pick);
            totalAllocated += qtyToPick;
            remaining -= qtyToPick;

            if (hoursLeft < 24) {
                warnings.add("URGENT: Batch " + batchId + " expires in " + hoursLeft + " hours!");
            } else if (hoursLeft < 72) {
                warnings.add("WARNING: Batch " + batchId + " expiring soon (" + (hoursLeft/24) + " days)");
            }

            em.createNativeQuery(
                "UPDATE inventory_batches SET reserved_qty = reserved_qty + ?1 WHERE id = ?2"
            ).setParameter(1, qtyToPick).setParameter(2, batchId).executeUpdate();
        }

        boolean success = totalAllocated >= requestedQty;
        String message = success ?
            "Picked " + totalAllocated + " units from " + picks.size() + " batches" :
            "Only " + totalAllocated + " available, need " + requestedQty;

        PickingResult result = new PickingResult(success, message, picks, totalAllocated);
        result.warnings = warnings;
        return result;
    }

    public List<ExpiryAlert> getExpiryAlerts(String warehouseId, int thresholdHours) {
        Query query = em.createNativeQuery(
            "SELECT b.id, b.product_id, p.name, b.quantity, b.expiry_date " +
            "FROM inventory_batches b JOIN products p ON b.product_id = p.id " +
            "WHERE b.warehouse_id = ?1 AND b.status = 'AVAILABLE' " +
            "ORDER BY b.expiry_date ASC"
        );
        query.setParameter(1, warehouseId);

        List<ExpiryAlert> alerts = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        @SuppressWarnings("unchecked")
        List<Object[]> alertList = (List<Object[]>) query.getResultList();
        for (Object[] row : alertList) {
            int batchId = ((Number) row[0]).intValue();
            String productId = (String) row[1];
            String productName = (String) row[2];
            int quantity = ((Number) row[3]).intValue();
            LocalDate expiryDate = ((java.sql.Date) row[4]).toLocalDate();

            long hoursRemaining = ChronoUnit.HOURS.between(now, expiryDate.atStartOfDay());

            if (hoursRemaining <= thresholdHours) {
                String alertLevel = hoursRemaining < 24 ? "CRITICAL" :
                                     hoursRemaining < 48 ? "HIGH" : "WARNING";

                alerts.add(new ExpiryAlert(
                    "BATCH-" + batchId, productId, productName, quantity,
                    expiryDate, hoursRemaining, alertLevel
                ));
            }
        }
        return alerts;
    }

    public Map<String, Object> getFEFOStats(String warehouseId) {
        Map<String, Object> stats = new HashMap<>();

        Query total = em.createNativeQuery(
            "SELECT COUNT(*), COALESCE(SUM(quantity - reserved_qty), 0) " +
            "FROM inventory_batches WHERE warehouse_id = ?1 AND status = 'AVAILABLE'"
        );
        total.setParameter(1, warehouseId);
        Object[] r = (Object[]) total.getSingleResult();
        stats.put("totalBatches", ((Number) r[0]).intValue());
        stats.put("totalStock", ((Number) r[1]).intValue());

        Query expiringSoon = em.createNativeQuery(
            "SELECT COUNT(*) FROM inventory_batches " +
            "WHERE warehouse_id = ?1 AND status = 'AVAILABLE' " +
            "AND expiry_date <= CURRENT_DATE + INTERVAL '3' DAY"
        );
        expiringSoon.setParameter(1, warehouseId);
        stats.put("expiringSoon", ((Number) expiringSoon.getSingleResult()).intValue());

        Query atRisk = em.createNativeQuery(
            "SELECT COUNT(*) FROM inventory_batches " +
            "WHERE warehouse_id = ?1 AND expiry_date <= CURRENT_DATE + INTERVAL '1' DAY"
        );
        atRisk.setParameter(1, warehouseId);
        stats.put("atRisk", ((Number) atRisk.getSingleResult()).intValue());

        Query expired = em.createNativeQuery(
            "SELECT COUNT(*) FROM inventory_batches " +
            "WHERE warehouse_id = ?1 AND expiry_date < CURRENT_DATE"
        );
        expired.setParameter(1, warehouseId);
        stats.put("expired", ((Number) expired.getSingleResult()).intValue());

        return stats;
    }

    private String getBatchStatus(long hoursRemaining) {
        if (hoursRemaining < 0) return "EXPIRED";
        if (hoursRemaining < 24) return "CRITICAL";
        if (hoursRemaining < 72) return "EXPIRING_SOON";
        if (hoursRemaining < 168) return "CHECKING";
        return "OK";
    }

    private String getRecommendation(long hoursRemaining) {
        if (hoursRemaining < 0) return "Remove from inventory immediately";
        if (hoursRemaining < 24) return "Pick immediately - expires today";
        if (hoursRemaining < 72) return "Prioritize in next batch";
        if (hoursRemaining < 168) return "Monitor closely";
        return "Stock healthy";
    }
}