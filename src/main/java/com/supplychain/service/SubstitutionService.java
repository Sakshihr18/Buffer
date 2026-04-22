package com.supplychain.service;

import org.springframework.stereotype.Service;
import jakarta.persistence.*;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

@Service
public class SubstitutionService {

    @PersistenceContext
    private EntityManager em;

    public static class SubstituteProduct {
        public String productId;
        public String productName;
        public double similarity;
        public boolean inStock;
        public int availableQty;
        public BigDecimal priceDifference;
        public String reason;

        public SubstituteProduct(String productId, String productName, double similarity,
                           boolean inStock, int availableQty, BigDecimal priceDifference, String reason) {
            this.productId = productId;
            this.productName = productName;
            this.similarity = similarity;
            this.inStock = inStock;
            this.availableQty = availableQty;
            this.priceDifference = priceDifference;
            this.reason = reason;
        }
    }

    public static class SubstitutionResult {
        public String requestedProductId;
        public String requestedProductName;
        public boolean isAvailable;
        public int requestedQty;
        public List<SubstituteProduct> substitutes;
        public SubstituteProduct primaryRecommendation;
        public String message;

        public SubstitutionResult(String requestedProductId, String requestedProductName, boolean isAvailable,
                               int requestedQty, List<SubstituteProduct> substitutes) {
            this.requestedProductId = requestedProductId;
            this.requestedProductName = requestedProductName;
            this.isAvailable = isAvailable;
            this.requestedQty = requestedQty;
            this.substitutes = substitutes;
            this.primaryRecommendation = !substitutes.isEmpty() ? substitutes.get(0) : null;
            this.message = isAvailable ? "Product available" :
                substitutes.isEmpty() ? "No substitutes available" :
                    "Found " + substitutes.size() + " alternatives";
        }
    }

    public SubstitutionResult checkSubstitution(String productId, int requestedQty) {
        Query productQuery = em.createNativeQuery(
            "SELECT name FROM products WHERE id = ?1"
        );
        productQuery.setParameter(1, productId);
        
        if (productQuery.getResultList().isEmpty()) {
            return new SubstitutionResult(productId, "Unknown", false, requestedQty, new ArrayList<>());
        }
        String productName = (String) productQuery.getSingleResult();

        Query stockQuery = em.createNativeQuery(
            "SELECT COALESCE(SUM(quantity - reserved_qty), 0) " +
            "FROM inventory_batches " +
            "WHERE product_id = ?1 AND status = 'AVAILABLE'"
        );
        stockQuery.setParameter(1, productId);
        int availableStock = ((Number) stockQuery.getSingleResult()).intValue();

        boolean isAvailable = availableStock >= requestedQty;

        List<SubstituteProduct> substitutes = new ArrayList<>();

        if (!isAvailable) {
            Query subQuery = em.createNativeQuery(
                "SELECT s.substitute_id, p.name, s.similarity, s.price_diff, s.rank, " +
                "COALESCE((SELECT SUM(i.quantity - i.reserved_qty) FROM inventory_batches i " +
                "       WHERE i.product_id = s.substitute_id AND i.status = 'AVAILABLE'), 0) as stock " +
                "FROM substitution_mapping s " +
                "JOIN products p ON s.substitute_id = p.id " +
                "WHERE s.product_id = ?1 AND s.is_active = TRUE " +
                "ORDER BY s.rank ASC"
            );
            subQuery.setParameter(1, productId);

            for (Object[] row : (List<Object[]>) subQuery.getResultList()) {
                String subId = (String) row[0];
                String subName = (String) row[1];
                double similarity = ((Number) row[2]).doubleValue();
                BigDecimal priceDiff = row[3] != null ? new BigDecimal(((Number) row[3]).doubleValue()) : BigDecimal.ZERO;
                int rank = ((Number) row[4]).intValue();
                int stock = ((Number) row[5]).intValue();

                boolean inStock = stock >= requestedQty;

                String reason = getSubstitutionReason(rank, similarity, priceDiff);

                substitutes.add(new SubstituteProduct(
                    subId, subName, similarity, inStock, stock, priceDiff, reason
                ));
            }
        }

        return new SubstitutionResult(productId, productName, isAvailable, requestedQty, substitutes);
    }

    public List<SubstituteProduct> getAllOutOfStockWithSubstitutes(String warehouseId) {
        List<SubstituteProduct> results = new ArrayList<>();

        Query query = em.createNativeQuery(
            "SELECT DISTINCT b.product_id, p.name, s.substitute_id, sp.name, " +
            "s.similarity, s.price_diff, " +
            "COALESCE((SELECT SUM(i.quantity - i.reserved_qty) FROM inventory_batches i " +
            "         WHERE i.product_id = s.substitute_id AND i.warehouse_id = ?1 AND i.status = 'AVAILABLE'), 0) as stock " +
            "FROM inventory_batches b " +
            "JOIN products p ON b.product_id = p.id " +
            "JOIN substitution_mapping s ON b.product_id = s.product_id AND s.is_active = TRUE " +
            "JOIN products sp ON s.substitute_id = sp.id " +
            "WHERE b.warehouse_id = ?1 " +
            "AND (SELECT SUM(quantity - reserved_qty) FROM inventory_batches i2 " +
            "     WHERE i2.product_id = b.product_id AND i2.warehouse_id = ?1) < 10 " +
            "ORDER BY s.rank"
        );
        query.setParameter(1, warehouseId);

        for (Object[] row : (List<Object[]>) query.getResultList()) {
            String productId = (String) row[0];
            String productName = (String) row[1];
            String subId = (String) row[2];
            String subName = (String) row[3];
            double similarity = ((Number) row[4]).doubleValue();
            BigDecimal priceDiff = new BigDecimal(((Number) row[5]).doubleValue());
            int stock = ((Number) row[6]).intValue();

            results.add(new SubstituteProduct(
                subId, subName, similarity, stock > 0, stock, priceDiff,
                "Alternative for " + productName
            ));
        }

        return results;
    }

    public Map<String, Object> getSubstitutionStats(String warehouseId) {
        Map<String, Object> stats = new HashMap<>();

        Query outOfStock = em.createNativeQuery(
            "SELECT COUNT(DISTINCT product_id) FROM inventory_batches " +
            "WHERE warehouse_id = ?1 AND status = 'AVAILABLE' " +
            "AND (quantity - reserved_qty) < 10"
        );
        outOfStock.setParameter(1, warehouseId);
        stats.put("outOfStockCount", ((Number) outOfStock.getSingleResult()).intValue());

        Query withSubs = em.createNativeQuery(
            "SELECT COUNT(DISTINCT s.product_id) " +
            "FROM substitution_mapping s " +
            "JOIN inventory_batches b ON s.product_id = b.product_id " +
            "WHERE b.warehouse_id = ?1 " +
            "AND (SELECT SUM(quantity - reserved_qty) FROM inventory_batches i2 " +
            "     WHERE i2.product_id = s.product_id AND i2.warehouse_id = ?1) < 10 " +
            "AND s.is_active = TRUE"
        );
        withSubs.setParameter(1, warehouseId);
        stats.put("withSubstitutes", ((Number) withSubs.getSingleResult()).intValue());

        return stats;
    }

    @Transactional
    public void addManualSubstitution(String productId, String substituteId, double similarity) {
        em.createNativeQuery(
            "INSERT INTO substitution_mapping (product_id, substitute_id, similarity, rank) " +
            "VALUES (?1, ?2, ?3, (SELECT COALESCE(MAX(rank), 0) + 1 FROM substitution_mapping WHERE product_id = ?1))"
        )
        .setParameter(1, productId)
        .setParameter(2, substituteId)
        .setParameter(3, similarity)
        .executeUpdate();
    }

    private String getSubstitutionReason(int rank, double similarity, BigDecimal priceDiff) {
        String reason = "Rank #" + rank + " alternative";
        
        if (similarity >= 0.95) {
            reason = "Direct substitute - nearly identical";
        } else if (similarity >= 0.85) {
            reason = "Similar product, same category";
        } else {
            reason = "Alternative product";
        }

        if (priceDiff.intValue() < 0) {
            reason += " (₹" + Math.abs(priceDiff.intValue()) + " cheaper)";
        } else if (priceDiff.intValue() > 0) {
            reason += " (₹" + priceDiff.intValue() + " expensive)";
        }

        return reason;
    }
}