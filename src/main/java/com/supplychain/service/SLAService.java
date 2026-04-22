package com.supplychain.service;

import org.springframework.stereotype.Service;
import jakarta.persistence.*;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
public class SLAService {

    @PersistenceContext
    private EntityManager em;

    public static class SLAOrder {
        public String orderId;
        public String customerId;
        public String status;
        public String priority;
        public LocalDateTime slaDeadline;
        public double slaScore;
        public long hoursRemaining;
        public String slaStatus;
        public String recommendation;
        public String assignedRider;

        public SLAOrder(String orderId, String customerId, String status, String priority,
                  LocalDateTime slaDeadline, double slaScore, long hoursRemaining,
                  String slaStatus, String recommendation, String assignedRider) {
            this.orderId = orderId;
            this.customerId = customerId;
            this.status = status;
            this.priority = priority;
            this.slaDeadline = slaDeadline;
            this.slaScore = slaScore;
            this.hoursRemaining = hoursRemaining;
            this.slaStatus = slaStatus;
            this.recommendation = recommendation;
            this.assignedRider = assignedRider;
        }
    }

    public List<SLAOrder> getActiveOrdersWithSLA(String warehouseId) {
        List<SLAOrder> orders = new ArrayList<>();

        Query query = em.createNativeQuery(
            "SELECT o.id, o.customer_id, o.status, o.priority, o.sla_deadline, o.sla_score, o.assigned_rider " +
            "FROM orders o " +
            "WHERE o.status IN ('PENDING','CONFIRMED','PICKING','PACKING') " +
            "AND o.sla_deadline IS NOT NULL " +
            "ORDER BY o.sla_score ASC, o.sla_deadline ASC"
        );

        LocalDateTime now = LocalDateTime.now();
        @SuppressWarnings("unchecked")
        List<Object[]> resultList = (List<Object[]>) query.getResultList();
        for (Object[] row : resultList) {
            String orderId = (String) row[0];
            String customerId = (String) row[1];
            String status = (String) row[2];
            String priority = (String) row[3];
            LocalDateTime slaDeadline = row[4] != null ? ((java.sql.Timestamp) row[4]).toLocalDateTime() : null;
            double slaScore = row[5] != null ? ((Number) row[5]).doubleValue() : 100;
            String riderId = (String) row[6];

            long hoursRemaining = slaDeadline != null ?
                (int) Duration.between(now, slaDeadline).toHours() : 999;

            String slaStatus = calculateSLAStatus(hoursRemaining, slaScore);
            String recommendation = getRecommendation(slaStatus, status, riderId != null);

            orders.add(new SLAOrder(
                orderId, customerId, status, priority,
                slaDeadline, slaScore, hoursRemaining,
                slaStatus, recommendation, riderId
            ));
        }

        return orders;
    }

    public Map<String, Object> getSLADashboard() {
        Map<String, Object> dashboard = new HashMap<>();

        Query total = em.createNativeQuery(
            "SELECT COUNT(*) FROM orders " +
            "WHERE status IN ('PENDING','CONFIRMED','PICKING','PACKING') " +
            "AND sla_deadline IS NOT NULL"
        );
        dashboard.put("totalActive", ((Number) total.getSingleResult()).intValue());

        Query breached = em.createNativeQuery(
            "SELECT COUNT(*) FROM orders " +
            "WHERE sla_deadline < NOW() AND status IN ('PENDING','CONFIRMED','PICKING','PACKING')"
        );
        dashboard.put("breached", ((Number) breached.getSingleResult()).intValue());

        Query delayed = em.createNativeQuery(
            "SELECT COUNT(*) FROM orders " +
            "WHERE sla_deadline BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL '1' HOUR) " +
            "AND status IN ('PENDING','CONFIRMED','PICKING','PACKING')"
        );
        dashboard.put("delayed", ((Number) delayed.getSingleResult()).intValue());

        Query atRisk = em.createNativeQuery(
            "SELECT COUNT(*) FROM orders " +
            "WHERE sla_deadline BETWEEN DATE_ADD(NOW(), INTERVAL '1' HOUR) AND DATE_ADD(NOW(), INTERVAL '3' HOUR) " +
            "AND status IN ('PENDING','CONFIRMED','PICKING','PACKING')"
        );
        dashboard.put("atRisk", ((Number) atRisk.getSingleResult()).intValue());

        Query onTrack = em.createNativeQuery(
            "SELECT COUNT(*) FROM orders " +
            "WHERE (sla_deadline > DATE_ADD(NOW(), INTERVAL '3' HOUR) OR sla_deadline IS NULL) " +
            "AND status IN ('PENDING','CONFIRMED','PICKING','PACKING')"
        );
        dashboard.put("onTrack", ((Number) onTrack.getSingleResult()).intValue());

        Query avgScore = em.createNativeQuery(
            "SELECT AVG(sla_score) FROM orders " +
            "WHERE status IN ('PENDING','CONFIRMED','PICKING','PACKING') AND sla_score IS NOT NULL"
        );
        Number avg = (Number) avgScore.getSingleResult();
        dashboard.put("avgSlaScore", avg != null ? Math.round(avg.doubleValue()) : 100);

        return dashboard;
    }

    @Transactional
    public void updateSLAScores() {
        LocalDateTime now = LocalDateTime.now();

        em.createNativeQuery(
            "UPDATE orders SET sla_score = CASE " +
            "WHEN sla_deadline IS NULL THEN 100 " +
            "WHEN sla_deadline < NOW() THEN 0 " +
            "WHEN TIMESTAMPDIFF(HOUR, NOW(), sla_deadline) < 1 THEN 10 " +
            "WHEN TIMESTAMPDIFF(HOUR, NOW(), sla_deadline) < 2 THEN 30 " +
            "WHEN TIMESTAMPDIFF(HOUR, NOW(), sla_deadline) < 4 THEN 50 " +
            "ELSE 100 END, " +
            "sla_status = CASE " +
            "WHEN sla_deadline IS NULL THEN 'ON_TRACK' " +
            "WHEN sla_deadline < NOW() THEN 'BREACHED' " +
            "WHEN TIMESTAMPDIFF(HOUR, NOW(), sla_deadline) < 2 THEN 'DELAYED' " +
            "WHEN TIMESTAMPDIFF(HOUR, NOW(), sla_deadline) < 4 THEN 'AT_RISK' " +
            "ELSE 'ON_TRACK' END " +
            "WHERE status IN ('PENDING','CONFIRMED','PICKING','PACKING')"
        ).executeUpdate();
    }

    @Transactional
    public List<String> getUrgentOrdersForReroute() {
        List<String> urgent = new ArrayList<>();

        Query query = em.createNativeQuery(
            "SELECT o.id FROM orders o " +
            "WHERE o.status IN ('PENDING','CONFIRMED','PICKING','PACKING') " +
            "AND o.sla_deadline < DATE_ADD(NOW(), INTERVAL '2' HOUR) " +
            "AND o.assigned_rider IS NOT NULL"
        );

        for (Object row : query.getResultList()) {
            urgent.add((String) row);
        }

        return urgent;
    }

    private String calculateSLAStatus(long hoursRemaining, double slaScore) {
        if (hoursRemaining < 0 || slaScore == 0) return "BREACHED";
        if (hoursRemaining < 1 || slaScore < 20) return "DELAYED";
        if (hoursRemaining < 4 || slaScore < 50) return "AT_RISK";
        return "ON_TRACK";
    }

    private String getRecommendation(String slaStatus, String orderStatus, boolean hasRider) {
        switch (slaStatus) {
            case "BREACHED":
                return "IMMEDIATE ACTION - Contact customer, offer refund";
            case "DELAYED":
                if (!hasRider) return "Assign fastest available rider";
                return "Expedite packing, priority delivery";
            case "AT_RISK":
                if (!hasRider) return "Assign rider immediately";
                return "Fast track packing";
            default:
                return "Normal processing";
        }
    }
}