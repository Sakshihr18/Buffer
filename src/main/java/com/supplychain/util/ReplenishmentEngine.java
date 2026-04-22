package com.supplychain.util;

import java.util.*;
import java.util.stream.Collectors;

public class ReplenishmentEngine {

    private static final int DEFAULT_WINDOW_SIZE = 14;

    public ReplenishmentEngine() {
    }

    public ReplenishmentEngine(int windowSize) {
    }

    public static class DemandData {
        private final String productId;
        private final Deque<Integer> demandWindow;
        private final Map<String, List<Integer>> demandHistory;

        public DemandData(String productId) {
            this.productId = productId;
            this.demandWindow = new ArrayDeque<>();
            this.demandHistory = new HashMap<>();
        }

        public String getProductId() { return productId; }
        public Deque<Integer> getDemandWindow() { return demandWindow; }
        public Map<String, List<Integer>> getDemandHistory() { return demandHistory; }

        public void addDemand(String dateKey, int quantity) {
            demandWindow.addLast(quantity);
            demandHistory.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(quantity);
            while (demandWindow.size() > DEFAULT_WINDOW_SIZE) {
                demandWindow.removeFirst();
            }
        }

        public int getTotalDemand() {
            return demandWindow.stream().mapToInt(Integer::intValue).sum();
        }
    }

    public static class ReplenishmentResult {
        private final String productId;
        private final double avgDailyDemand;
        private final double demandStdDev;
        private final int safetyStock;
        private final int recommendedReorderQty;
        private final double urgencyScore;
        private final String alertType;
        private final String recommendation;

        public ReplenishmentResult(String productId, double avgDailyDemand, double demandStdDev,
                                  int safetyStock, int recommendedReorderQty, double urgencyScore,
                                  String alertType, String recommendation) {
            this.productId = productId;
            this.avgDailyDemand = avgDailyDemand;
            this.demandStdDev = demandStdDev;
            this.safetyStock = safetyStock;
            this.recommendedReorderQty = recommendedReorderQty;
            this.urgencyScore = urgencyScore;
            this.alertType = alertType;
            this.recommendation = recommendation;
        }

        public String getProductId() { return productId; }
        public double getAvgDailyDemand() { return avgDailyDemand; }
        public double getDemandStdDev() { return demandStdDev; }
        public int getSafetyStock() { return safetyStock; }
        public int getRecommendedReorderQty() { return recommendedReorderQty; }
        public double getUrgencyScore() { return urgencyScore; }
        public String getAlertType() { return alertType; }
        public String getRecommendation() { return recommendation; }
    }

    public double averageDemand(List<Integer> values) {
        if (values == null || values.isEmpty()) return 0.0;
        return values.stream().mapToInt(Integer::intValue).average().orElse(0.0);
    }

    public double avgDailyDemand(List<Integer> demandWindow) {
        return averageDemand(demandWindow);
    }

    public double demandStdDev(List<Integer> demandWindow) {
        if (demandWindow == null || demandWindow.isEmpty()) return 0.0;
        double avg = averageDemand(demandWindow);
        double variance = demandWindow.stream()
            .mapToDouble(v -> (v - avg) * (v - avg))
            .average()
            .orElse(0.0);
        return Math.sqrt(variance);
    }

    public int safetyStock(double avgDailyDemand, int leadTimeDays, double bufferPercent) {
        double base = avgDailyDemand * leadTimeDays;
        double buffer = base * bufferPercent;
        return (int) Math.ceil(base + buffer);
    }

    public int autoReorderQty(double avgDailyDemand, double stdDev, int leadTimeDays,
                    double serviceLevelZ, int currentStock, int targetStock) {
        int rawReorder = targetStock - currentStock;
        return Math.max(0, rawReorder);
    }

    public int calculateReorderQty(double avgDailyDemand, double stdDev, int leadTimeDays,
                                  double serviceLevelZ, int safetyStock, int currentStock) {
        double baseDemand = avgDailyDemand * leadTimeDays;
        double safetyBuffer = serviceLevelZ * stdDev * Math.sqrt(leadTimeDays);
        int targetLevel = (int) Math.ceil(baseDemand + safetyBuffer);
        int reorderQty = targetLevel - currentStock;
        return Math.max(0, reorderQty);
    }

    public ReplenishmentResult evaluate(String productId, int currentStock, int leadTimeDays,
                                    double bufferPercent, double serviceLevelZ,
                                    Map<String, Integer> recentDemand) {
        List<Integer> demandValues = recentDemand != null ?
            new ArrayList<>(recentDemand.values()) : new ArrayList<>();

        double avg = avgDailyDemand(demandValues);
        double stdDev = demandStdDev(demandValues);
        int calculatedSafetyStock = safetyStock(avg, leadTimeDays, bufferPercent);
        int reorderQty = calculateReorderQty(avg, stdDev, leadTimeDays,
            serviceLevelZ, calculatedSafetyStock, currentStock);

        String alertType;
        double urgencyScore;
        String recommendation;

        if (currentStock <= 0) {
            alertType = "CRITICAL_OUT_OF_STOCK";
            urgencyScore = 100.0;
            recommendation = "Emergency reorder required immediately. Consider expedited shipping.";
        } else if (currentStock < calculatedSafetyStock * 0.5) {
            alertType = "CRITICAL_LOW_STOCK";
            urgencyScore = 80.0;
            recommendation = "Rush reorder: stock below 50% of safety stock.";
        } else if (currentStock < calculatedSafetyStock) {
            alertType = "LOW_STOCK";
            urgencyScore = 50.0;
            recommendation = "Standard reorder recommended.";
        } else if (currentStock < calculatedSafetyStock * 1.5) {
            alertType = "WARNING";
            urgencyScore = 25.0;
            recommendation = "Monitor closely, prepare for reorder.";
        } else {
            alertType = "OK";
            urgencyScore = 0.0;
            recommendation = "Stock levels normal.";
        }

        return new ReplenishmentResult(productId, avg, stdDev, calculatedSafetyStock,
            reorderQty, urgencyScore, alertType, recommendation);
    }

    public List<ReplenishmentResult> batchEvaluate(Map<String, Integer> stockLevels,
                                              Map<String, Map<String, Integer>> demandHistoryMap,
                                              int defaultLeadTime,
                                              double bufferPercent,
                                              double serviceLevelZ) {
        return stockLevels.entrySet().stream()
            .map(e -> evaluate(e.getKey(), e.getValue(), defaultLeadTime,
                bufferPercent, serviceLevelZ, demandHistoryMap.get(e.getKey())))
            .sorted(Comparator.comparingDouble(ReplenishmentResult::getUrgencyScore).reversed())
            .collect(Collectors.toList());
    }

    public PriorityQueue<ReplenishmentResult> getReorderQueue(List<ReplenishmentResult> results) {
        PriorityQueue<ReplenishmentResult> queue = new PriorityQueue<>(
            (a, b) -> Double.compare(b.getUrgencyScore(), a.getUrgencyScore())
        );
        queue.addAll(results.stream()
            .filter(r -> r.getRecommendedReorderQty() > 0)
            .collect(Collectors.toList()));
        return queue;
    }

    public String generateReorderReport(List<ReplenishmentResult> results) {
        StringBuilder report = new StringBuilder();
        report.append("=== REPLENISHMENT REPORT ===\n");

        long critical = results.stream().filter(r -> "CRITICAL_OUT_OF_STOCK".equals(r.getAlertType())).count();
        long lowStock = results.stream().filter(r -> "CRITICAL_LOW_STOCK".equals(r.getAlertType())).count();
        long warning = results.stream().filter(r -> "LOW_STOCK".equals(r.getAlertType())).count();

        report.append(String.format("Critical: %d | Low Stock: %d | Warning: %d\n\n", critical, lowStock, warning));

        results.stream()
            .filter(r -> r.getRecommendedReorderQty() > 0)
            .forEach(r -> report.append(String.format("[%s] %s: Reorder %d units (Current: %s)\n",
                r.getAlertType(), r.getProductId(), r.getRecommendedReorderQty(), r.getRecommendation())));

        return report.toString();
    }
}