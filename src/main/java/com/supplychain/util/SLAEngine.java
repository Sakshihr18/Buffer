package com.supplychain.util;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class SLAEngine {

    public static class SLAStatus {
        private final String orderId;
        private final LocalDateTime deadline;
        private final double priorityScore;
        private final long hoursRemaining;
        private final double urgencyLevel;
        private final String status;
        private final String recommendation;

        public SLAStatus(String orderId, LocalDateTime deadline,
                  double priorityScore, long hoursRemaining,
                  double urgencyLevel, String status, String recommendation) {
            this.orderId = orderId;
            this.deadline = deadline;
            this.priorityScore = priorityScore;
            this.hoursRemaining = hoursRemaining;
            this.urgencyLevel = urgencyLevel;
            this.status = status;
            this.recommendation = recommendation;
        }

        public String getOrderId() { return orderId; }
        public LocalDateTime getDeadline() { return deadline; }
        public double getPriorityScore() { return priorityScore; }
        public long getHoursRemaining() { return hoursRemaining; }
        public double getUrgencyLevel() { return urgencyLevel; }
        public String getStatus() { return status; }
        public String getRecommendation() { return recommendation; }
    }

    public static class SLAScheduleResult {
        private final String orderId;
        private final int schedulePosition;
        private final double priorityScore;
        private final String scheduleGroup;
        private final String estimatedDelivery;
        private final boolean canMeetSLA;
        private final String reasoning;

        public SLAScheduleResult(String orderId, int schedulePosition, double priorityScore,
                            String scheduleGroup, String estimatedDelivery,
                            boolean canMeetSLA, String reasoning) {
            this.orderId = orderId;
            this.schedulePosition = schedulePosition;
            this.priorityScore = priorityScore;
            this.scheduleGroup = scheduleGroup;
            this.estimatedDelivery = estimatedDelivery;
            this.canMeetSLA = canMeetSLA;
            this.reasoning = reasoning;
        }

        public String getOrderId() { return orderId; }
        public int getSchedulePosition() { return schedulePosition; }
        public double getPriorityScore() { return priorityScore; }
        public String getScheduleGroup() { return scheduleGroup; }
        public String getEstimatedDelivery() { return estimatedDelivery; }
        public boolean canMeetSLA() { return canMeetSLA; }
        public String getReasoning() { return reasoning; }
    }

    public enum Priority { HIGH, MEDIUM, LOW }

    public static class OrderScheduleItem {
        private final String orderId;
        private final String productId;
        private final int quantity;
        private final Priority priority;
        private final LocalDateTime deadline;
        private final LocalDateTime createdAt;
        private double calculatedScore;
        private int schedulePosition;

        public OrderScheduleItem(String orderId, String productId, int quantity,
                          Priority priority, LocalDateTime deadline, LocalDateTime createdAt) {
            this.orderId = orderId;
            this.productId = productId;
            this.quantity = quantity;
            this.priority = priority;
            this.deadline = deadline;
            this.createdAt = createdAt;
        }

        public String getOrderId() { return orderId; }
        public String getProductId() { return productId; }
        public int getQuantity() { return quantity; }
        public Priority getPriority() { return priority; }
        public LocalDateTime getDeadline() { return deadline; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public double getCalculatedScore() { return calculatedScore; }
        public void setCalculatedScore(double calculatedScore) { this.calculatedScore = calculatedScore; }
        public int getSchedulePosition() { return schedulePosition; }
        public void setSchedulePosition(int schedulePosition) { this.schedulePosition = schedulePosition; }
    }

    private static final double PRIORITY_WEIGHT = 40.0;
    private static final double DEADLINE_WEIGHT = 40.0;
    private static final double AGE_WEIGHT = 20.0;

    private static final double CRITICAL_THRESHOLD = 75.0;
    private static final double URGENT_THRESHOLD = 50.0;
    private static final double WARNING_THRESHOLD = 25.0;

    public double calculatePriorityScore(String orderId, Priority priority,
                                     LocalDateTime deadline, LocalDateTime createdAt,
                                     LocalDateTime currentTime) {
        double priorityScore = getBasePriorityScore(priority);
        double deadlineScore = calculateDeadlineScore(deadline, currentTime);
        double ageScore = calculateAgeScore(createdAt, currentTime);

        return (priorityScore * PRIORITY_WEIGHT / 100) +
               (deadlineScore * DEADLINE_WEIGHT / 100) +
               (ageScore * AGE_WEIGHT / 100);
    }

    private double getBasePriorityScore(Priority priority) {
        switch (priority) {
            case HIGH: return 100.0;
            case MEDIUM: return 60.0;
            case LOW: return 30.0;
            default: return 50.0;
        }
    }

    public double calculateDeadlineScore(LocalDateTime deadline, LocalDateTime currentTime) {
        if (deadline == null) return 50.0;

        long hoursRemaining = ChronoUnit.HOURS.between(currentTime, deadline);

        if (hoursRemaining <= 0) return 100.0;
        if (hoursRemaining <= 4) return 90.0;
        if (hoursRemaining <= 12) return 75.0;
        if (hoursRemaining <= 24) return 60.0;
        if (hoursRemaining <= 48) return 40.0;
        if (hoursRemaining <= 72) return 25.0;
        return 10.0;
    }

    public double calculateAgeScore(LocalDateTime createdAt, LocalDateTime currentTime) {
        if (createdAt == null) return 0.0;

        long hoursOld = ChronoUnit.HOURS.between(createdAt, currentTime);

        if (hoursOld <= 1) return 10.0;
        if (hoursOld <= 4) return 30.0;
        if (hoursOld <= 12) return 50.0;
        if (hoursOld <= 24) return 70.0;
        if (hoursOld <= 48) return 85.0;
        return 100.0;
    }

    public SLAStatus evaluateOrder(String orderId, Priority priority,
                               LocalDateTime deadline, LocalDateTime createdAt) {
        LocalDateTime now = LocalDateTime.now();
        double priorityScore = calculatePriorityScore(orderId, priority, deadline, createdAt, now);
        long hoursRemaining = deadline != null ? ChronoUnit.HOURS.between(now, deadline) : Long.MAX_VALUE;

        double urgencyLevel = priorityScore;
        String status;
        String recommendation;

        if (hoursRemaining <= 0) {
            status = "BREACHED";
            recommendation = "Order SLA breached! Immediate action required.";
        } else if (urgencyLevel >= CRITICAL_THRESHOLD) {
            status = "CRITICAL";
            recommendation = "Expedite delivery - schedule immediately.";
        } else if (urgencyLevel >= URGENT_THRESHOLD) {
            status = "URGENT";
            recommendation = "Prioritize in next batch.";
        } else if (urgencyLevel >= WARNING_THRESHOLD) {
            status = "AT_RISK";
            recommendation = "Schedule within 24 hours.";
        } else {
            status = "ON_TRACK";
            recommendation = "Standard scheduling adequate.";
        }

        return new SLAStatus(orderId, deadline, priorityScore,
            hoursRemaining, urgencyLevel, status, recommendation);
    }

    public List<SLAScheduleResult> scheduleOrders(List<OrderScheduleItem> orders) {
        LocalDateTime now = LocalDateTime.now();

        for (OrderScheduleItem order : orders) {
            double score = calculatePriorityScore(
                order.getOrderId(),
                order.getPriority(),
                order.getDeadline(),
                order.getCreatedAt(),
                now
            );
            order.setCalculatedScore(score);
        }

        List<OrderScheduleItem> sorted = orders.stream()
            .sorted(Comparator.comparingDouble(OrderScheduleItem::getCalculatedScore).reversed())
            .collect(Collectors.toList());

        List<SLAScheduleResult> results = new ArrayList<>();
        int position = 1;

        for (OrderScheduleItem order : sorted) {
            double score = order.getCalculatedScore();
            String group;
            String estimatedDelivery;

            if (score >= CRITICAL_THRESHOLD) {
                group = "CRITICAL";
                estimatedDelivery = now.plusHours(2).toString();
            } else if (score >= URGENT_THRESHOLD) {
                group = "URGENT";
                estimatedDelivery = now.plusHours(6).toString();
            } else if (score >= WARNING_THRESHOLD) {
                group = "STANDARD";
                estimatedDelivery = now.plusHours(24).toString();
            } else {
                group = "BATCH";
                estimatedDelivery = now.plusHours(48).toString();
            }

            String reasoning = String.format("Priority %s + Deadline score: %.1f",
                order.getPriority(), score);

            results.add(new SLAScheduleResult(
                order.getOrderId(),
                position++,
                score,
                group,
                estimatedDelivery,
                true,
                reasoning
            ));
        }

        return results;
    }

    public PriorityQueue<OrderScheduleItem> createPriorityQueue(List<OrderScheduleItem> orders) {
        PriorityQueue<OrderScheduleItem> queue = new PriorityQueue<>(
            100, (a, b) -> Double.compare(b.getCalculatedScore(), a.getCalculatedScore())
        );

        LocalDateTime now = LocalDateTime.now();
        for (OrderScheduleItem order : orders) {
            double score = calculatePriorityScore(
                order.getOrderId(),
                order.getPriority(),
                order.getDeadline(),
                order.getCreatedAt(),
                now
            );
            order.setCalculatedScore(score);
            queue.add(order);
        }

        return queue;
    }

    public List<SLAStatus> batchEvaluate(Map<String, Object> ordersData) {
        List<SLAStatus> results = new ArrayList<>();

        for (Map.Entry<String, Object> entry : ordersData.entrySet()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> orderData = (Map<String, Object>) entry.getValue();

            String orderId = entry.getKey();
            Priority priority = Priority.valueOf(
                (String) orderData.getOrDefault("priority", "MEDIUM")
            );
            LocalDateTime deadline = orderData.get("deadline") != null ?
                LocalDateTime.parse((String) orderData.get("deadline")) : null;
            LocalDateTime createdAt = orderData.get("createdAt") != null ?
                LocalDateTime.parse((String) orderData.get("createdAt")) : LocalDateTime.now();

            results.add(evaluateOrder(orderId, priority, deadline, createdAt));
        }

        return results.stream()
            .sorted(Comparator.comparingDouble(SLAStatus::getUrgencyLevel).reversed())
            .collect(Collectors.toList());
    }

    public String generateSLAReport(List<SLAStatus> statuses) {
        StringBuilder report = new StringBuilder();
        report.append("=== SLA SCHEDULING REPORT ===\n\n");

        long breached = statuses.stream().filter(s -> "BREACHED".equals(s.getStatus())).count();
        long critical = statuses.stream().filter(s -> "CRITICAL".equals(s.getStatus())).count();
        long urgent = statuses.stream().filter(s -> "URGENT".equals(s.getStatus())).count();

        report.append(String.format("Breached: %d | Critical: %d | Urgent: %d\n\n",
            breached, critical, urgent));

        statuses.stream()
            .filter(s -> !"ON_TRACK".equals(s.getStatus()))
            .forEach(s -> report.append(String.format("[%s] %s: %s\n  -> %s\n",
                s.getStatus(), s.getOrderId(), s.getRecommendation(),
                String.format("Hours remaining: %d, Priority: %.1f",
                    s.getHoursRemaining(), s.getPriorityScore()))));

        return report.toString();
    }

    public static Map<Priority, Integer> getDefaultProcessingTimes() {
        Map<Priority, Integer> times = new HashMap<>();
        times.put(Priority.HIGH, 2);
        times.put(Priority.MEDIUM, 6);
        times.put(Priority.LOW, 24);
        return times;
    }

    public boolean canFulfillSLA(OrderScheduleItem order, int processingCapacityPerHour) {
        LocalDateTime now = LocalDateTime.now();
        long hoursRemaining = ChronoUnit.HOURS.between(now, order.getDeadline());

        if (hoursRemaining <= 0) return false;

        double requiredHours = order.getQuantity() / (double) processingCapacityPerHour;
        return hoursRemaining >= requiredHours;
    }
}