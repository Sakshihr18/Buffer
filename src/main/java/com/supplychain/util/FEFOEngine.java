package com.supplychain.util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class FEFOEngine {

    public static class InventoryBatch {
        private final String batchId;
        private final String productId;
        private final int quantity;
        private final LocalDate manufacturingDate;
        private final LocalDate expiryDate;
        private final LocalDate receivedDate;

        public InventoryBatch(String batchId, String productId, int quantity,
                         LocalDate manufacturingDate, LocalDate expiryDate) {
            this.batchId = batchId;
            this.productId = productId;
            this.quantity = quantity;
            this.manufacturingDate = manufacturingDate;
            this.expiryDate = expiryDate;
            this.receivedDate = LocalDate.now();
        }

        public InventoryBatch(String batchId, String productId, int quantity,
                         LocalDate manufacturingDate, LocalDate expiryDate, LocalDate receivedDate) {
            this.batchId = batchId;
            this.productId = productId;
            this.quantity = quantity;
            this.manufacturingDate = manufacturingDate;
            this.expiryDate = expiryDate;
            this.receivedDate = receivedDate;
        }

        public String getBatchId() { return batchId; }
        public String getProductId() { return productId; }
        public int getQuantity() { return quantity; }
        public LocalDate getManufacturingDate() { return manufacturingDate; }
        public LocalDate getExpiryDate() { return expiryDate; }
        public LocalDate getReceivedDate() { return receivedDate; }

        public long getDaysUntilExpiry() {
            return ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
        }

        public long getDaysSinceManufacturing() {
            return ChronoUnit.DAYS.between(manufacturingDate, LocalDate.now());
        }

        public boolean isExpired() {
            return expiryDate != null && LocalDate.now().isAfter(expiryDate);
        }

        public boolean isExpiringSoon(int thresholdDays) {
            return expiryDate != null &&
                   !isExpired() &&
                   getDaysUntilExpiry() <= thresholdDays;
        }

        public String getShelfStatus() {
            if (isExpired()) return "EXPIRED";
            long days = getDaysUntilExpiry();
            if (days <= 7) return "CRITICAL";
            if (days <= 30) return "EXPIRING_SOON";
            return "OK";
        }
    }

    public static class FEFORecommendation {
        private final String productId;
        private final String batchId;
        private final int quantityPicked;
        private final LocalDate expiryDate;
        private final long daysUntilExpiry;
        private final String rationale;
        private final String alternativeBatchId;
        private final int alternativeQty;

        public FEFORecommendation(String productId, String batchId, int quantityPicked,
                              LocalDate expiryDate, long daysUntilExpiry, String rationale,
                              String alternativeBatchId, int alternativeQty) {
            this.productId = productId;
            this.batchId = batchId;
            this.quantityPicked = quantityPicked;
            this.expiryDate = expiryDate;
            this.daysUntilExpiry = daysUntilExpiry;
            this.rationale = rationale;
            this.alternativeBatchId = alternativeBatchId;
            this.alternativeQty = alternativeQty;
        }

        public String getProductId() { return productId; }
        public String getBatchId() { return batchId; }
        public int getQuantityPicked() { return quantityPicked; }
        public LocalDate getExpiryDate() { return expiryDate; }
        public long getDaysUntilExpiry() { return daysUntilExpiry; }
        public String getRationale() { return rationale; }
        public String getAlternativeBatchId() { return alternativeBatchId; }
        public int getAlternativeQty() { return alternativeQty; }
    }

    public static class PickingResult {
        private final String productId;
        private final int totalRequested;
        private final List<FEFORecommendation> picks;
        private final List<String> warnings;
        private final boolean success;
        private final String message;

        public PickingResult(String productId, int totalRequested,
                          List<FEFORecommendation> picks, List<String> warnings,
                          boolean success, String message) {
            this.productId = productId;
            this.totalRequested = totalRequested;
            this.picks = picks;
            this.warnings = warnings;
            this.success = success;
            this.message = message;
        }

        public String getProductId() { return productId; }
        public int getTotalRequested() { return totalRequested; }
        public List<FEFORecommendation> getPicks() { return picks; }
        public List<String> getWarnings() { return warnings; }
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }

    private final Map<String, PriorityQueue<InventoryBatch>> batchesByProduct;

    public FEFOEngine() {
        this.batchesByProduct = new HashMap<>();
    }

    public void registerBatch(InventoryBatch batch) {
        batchesByProduct.computeIfAbsent(batch.getProductId(), k ->
            new PriorityQueue<>(Comparator.comparing(InventoryBatch::getExpiryDate)));
    }

    public void addBatch(String batchId, String productId, int quantity,
                     LocalDate manufacturingDate, LocalDate expiryDate) {
        InventoryBatch batch = new InventoryBatch(batchId, productId, quantity,
            manufacturingDate, expiryDate);
        batchesByProduct.computeIfAbsent(productId, k ->
            new PriorityQueue<>(Comparator.comparing(InventoryBatch::getExpiryDate)));
        batchesByProduct.get(productId).add(batch);
    }

    public void addBatch(String batchId, String productId, int quantity,
                     LocalDate manufacturingDate, LocalDate expiryDate, LocalDate receivedDate) {
        InventoryBatch batch = new InventoryBatch(batchId, productId, quantity,
            manufacturingDate, expiryDate, receivedDate);
        batchesByProduct.computeIfAbsent(productId, k ->
            new PriorityQueue<>(Comparator.comparing(InventoryBatch::getExpiryDate)));
        batchesByProduct.get(productId).add(batch);
    }

    public boolean removeBatch(String productId, String batchId, int qtyToRemove) {
        PriorityQueue<InventoryBatch> batches = batchesByProduct.get(productId);
        if (batches == null) return false;

        List<InventoryBatch> toRemove = new ArrayList<>();
        int remaining = qtyToRemove;

        for (InventoryBatch batch : batches) {
            if (batch.getBatchId().equals(batchId)) {
                if (batch.getQuantity() >= remaining) {
                    toRemove.add(batch);
                    remaining = 0;
                } else {
                    remaining -= batch.getQuantity();
                    toRemove.add(batch);
                }
                break;
            }
        }

        toRemove.forEach(batches::remove);
        return remaining == 0;
    }

    public List<InventoryBatch> getBatchesForProduct(String productId) {
        PriorityQueue<InventoryBatch> batches = batchesByProduct.get(productId);
        if (batches == null) return new ArrayList<>();
        return new ArrayList<>(batches);
    }

    public InventoryBatch getNextBatchToPick(String productId) {
        PriorityQueue<InventoryBatch> batches = batchesByProduct.get(productId);
        if (batches == null || batches.isEmpty()) return null;

        for (InventoryBatch batch : batches) {
            if (!batch.isExpired() && batch.getQuantity() > 0) {
                return batch;
            }
        }
        return null;
    }

    public PickingResult pick(String productId, int requestedQty) {
        return pick(productId, requestedQty, 30);
    }

    public PickingResult pick(String productId, int requestedQty, int expiryWarningDays) {
        PriorityQueue<InventoryBatch> batches = batchesByProduct.get(productId);
        List<FEFORecommendation> picks = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (batches == null || batches.isEmpty()) {
            return new PickingResult(productId, requestedQty, picks, warnings, false,
                "No batches available for product: " + productId);
        }

        int remaining = requestedQty;
        List<InventoryBatch> selectedBatches = new ArrayList<>();

        for (InventoryBatch batch : batches) {
            if (batch.isExpired()) {
                warnings.add("Batch " + batch.getBatchId() + " is EXPIRED!");
                continue;
            }

            if (batch.getQuantity() <= 0) continue;

            int qtyFromBatch = Math.min(batch.getQuantity(), remaining);
            picks.add(new FEFORecommendation(
                productId,
                batch.getBatchId(),
                qtyFromBatch,
                batch.getExpiryDate(),
                batch.getDaysUntilExpiry(),
                String.format("Batch %s expires in %d days",
                    batch.getBatchId(), batch.getDaysUntilExpiry()),
                null, 0
            ));

            remaining -= qtyFromBatch;
            selectedBatches.add(batch);

            if (batch.isExpiringSoon(expiryWarningDays)) {
                warnings.add(String.format("Batch %s expiring in %d days (%s)",
                    batch.getBatchId(), batch.getDaysUntilExpiry(), batch.getShelfStatus()));
            }

            if (remaining == 0) break;
        }

        if (remaining > 0) {
            return new PickingResult(productId, requestedQty, picks, warnings, false,
                String.format("Insufficient stock. Requested: %d, Available: %d",
                    requestedQty, requestedQty - remaining));
        }

        selectedBatches.forEach(batches::remove);

        String message = picks.isEmpty() ?
            "No valid batches found" :
            String.format("Picked %d units from %d batches",
                picks.stream().mapToInt(FEFORecommendation::getQuantityPicked).sum(),
                picks.size());

        return new PickingResult(productId, requestedQty, picks, warnings, true, message);
    }

    public FEFORecommendation recommendNextPick(String productId) {
        InventoryBatch next = getNextBatchToPick(productId);
        if (next == null) return null;

        return new FEFORecommendation(
            productId,
            next.getBatchId(),
            next.getQuantity(),
            next.getExpiryDate(),
            next.getDaysUntilExpiry(),
            String.format("Use batch %s (expires in %d days)",
                next.getBatchId(), next.getDaysUntilExpiry()),
            null, 0
        );
    }

    public List<InventoryBatch> getExpiringBatches(int daysThreshold) {
        List<InventoryBatch> expiring = new ArrayList<>();
        for (PriorityQueue<InventoryBatch> pq : batchesByProduct.values()) {
            for (InventoryBatch batch : pq) {
                if (batch.isExpiringSoon(daysThreshold)) {
                    expiring.add(batch);
                }
            }
        }
        return expiring.stream()
            .sorted(Comparator.comparing(InventoryBatch::getExpiryDate))
            .collect(Collectors.toList());
    }

    public List<InventoryBatch> getExpiredBatches() {
        List<InventoryBatch> expired = new ArrayList<>();
        for (PriorityQueue<InventoryBatch> pq : batchesByProduct.values()) {
            for (InventoryBatch batch : pq) {
                if (batch.isExpired()) {
                    expired.add(batch);
                }
            }
        }
        return expired;
    }

    public int getTotalQuantity(String productId) {
        PriorityQueue<InventoryBatch> batches = batchesByProduct.get(productId);
        if (batches == null) return 0;
        return batches.stream()
            .filter(b -> !b.isExpired())
            .mapToInt(InventoryBatch::getQuantity)
            .sum();
    }

    public Map<String, List<InventoryBatch>> getAllBatchesGrouped() {
        Map<String, List<InventoryBatch>> result = new HashMap<>();
        for (Map.Entry<String, PriorityQueue<InventoryBatch>> entry : batchesByProduct.entrySet()) {
            result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return result;
    }

    public String generateFEFOReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== FEFO INVENTORY REPORT ===\n\n");

        List<InventoryBatch> expired = getExpiredBatches();
        if (!expired.isEmpty()) {
            report.append("EXPIRED BATCHES:\n");
            expired.forEach(b -> report.append(String.format("  - %s: %s (expired %s)\n",
                b.getBatchId(), b.getProductId(), b.getExpiryDate())));
            report.append("\n");
        }

        List<InventoryBatch> expiring = getExpiringBatches(30);
        if (!expiring.isEmpty()) {
            report.append("EXPIRING SOON (next 30 days):\n");
            expiring.forEach(b -> report.append(String.format("  - %s: %s (expires in %d days)\n",
                b.getBatchId(), b.getProductId(), b.getDaysUntilExpiry())));
        }

        return report.toString();
    }

    public void clear() {
        batchesByProduct.clear();
    }

    public int getProductCount() {
        return batchesByProduct.size();
    }
}