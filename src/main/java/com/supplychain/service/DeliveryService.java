package com.supplychain.service;

import org.springframework.stereotype.Service;
import jakarta.persistence.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class DeliveryService {

    @PersistenceContext
    private EntityManager em;

    public static class RiderLocation {
        public String riderId;
        public String riderName;
        public double distanceKm;
        public double currentX;
        public double currentY;
        public String status;
        public double rating;
        public int todayOrders;
        public String estimatedPickup;
        public String vehicleType;

        public RiderLocation(String riderId, String riderName, double distanceKm, double currentX, double currentY,
                       String status, double rating, int todayOrders, String estimatedPickup, String vehicleType) {
            this.riderId = riderId;
            this.riderName = riderName;
            this.distanceKm = distanceKm;
            this.currentX = currentX;
            this.currentY = currentY;
            this.status = status;
            this.rating = rating;
            this.todayOrders = todayOrders;
            this.estimatedPickup = estimatedPickup;
            this.vehicleType = vehicleType;
        }
    }

    public static class DeliveryAssignment {
        public String orderId;
        public RiderLocation bestRider;
        public List<RiderLocation> alternatives;
        public double estimatedDeliveryTime;
        public String routeRecommendation;

        public DeliveryAssignment(String orderId, RiderLocation bestRider, List<RiderLocation> alternatives,
                             double estimatedDeliveryTime, String routeRecommendation) {
            this.orderId = orderId;
            this.bestRider = bestRider;
            this.alternatives = alternatives;
            this.estimatedDeliveryTime = estimatedDeliveryTime;
            this.routeRecommendation = routeRecommendation;
        }
    }

    public List<RiderLocation> findNearestRiders(String orderId, int limit, double radiusKm) {
        Query orderQuery = em.createNativeQuery(
            "SELECT delivery_x, delivery_y FROM orders WHERE id = ?1"
        );
        orderQuery.setParameter(1, orderId);

        if (orderQuery.getResultList().isEmpty()) {
            return new ArrayList<>();
        }

        Object[] orderLoc = (Object[]) orderQuery.getSingleResult();
        double destX = ((Number) orderLoc[0]).doubleValue();
        double destY = ((Number) orderLoc[1]).doubleValue();

        double radLat = radiusKm / 111.0; 
        double minLat = destX - radLat;
        double maxLat = destX + radLat;
        double minLng = destY - (radiusKm / (111.0 * Math.cos(Math.toRadians(destX))));
        double maxLng = destY + (radiusKm / (111.0 * Math.cos(Math.toRadians(destX))));

        Query riderQuery = em.createNativeQuery(
            "SELECT a.id, a.name, a.current_x, a.current_y, a.status, a.rating, a.today_orders, a.vehicle_type " +
            "FROM delivery_agents a " +
            "JOIN warehouses w ON a.warehouse_id = w.id " +
            "WHERE a.is_active = TRUE " +
            "AND a.status IN ('AVAILABLE','BUSY') " +
            "AND a.current_x BETWEEN ?1 AND ?2 " +
            "AND a.current_y BETWEEN ?3 AND ?4 " +
            "ORDER BY " +
            "CASE a.status WHEN 'AVAILABLE' THEN 0 ELSE 1 END, " +
            "a.today_orders ASC"
        );
        riderQuery.setParameter(1, minLat);
        riderQuery.setParameter(2, maxLat);
        riderQuery.setParameter(3, minLng);
        riderQuery.setParameter(4, maxLng);
        riderQuery.setMaxResults(limit);

        List<RiderLocation> riders = new ArrayList<>();

        @SuppressWarnings("unchecked")
        List<Object[]> riderList = (List<Object[]>) riderQuery.getResultList();
        for (Object[] row : riderList) {
            String riderId = (String) row[0];
            String riderName = (String) row[1];
            double riderX = row[2] != null ? ((Number) row[2]).doubleValue() : destX;
            double riderY = row[3] != null ? ((Number) row[3]).doubleValue() : destY;
            String status = (String) row[4];
            double rating = row[5] != null ? ((Number) row[5]).doubleValue() : 4.5;
            int todayOrders = row[6] != null ? ((Number) row[6]).intValue() : 0;
            String vehicleType = row[7] != null ? (String) row[7] : "SCOOTY";

            double distance = calculateDistance(riderX, riderY, destX, destY);
            int pickupMins = (int) (distance * 4);

            riders.add(new RiderLocation(
                riderId, riderName, Math.round(distance * 10) / 10.0,
                riderX, riderY, status, rating, todayOrders,
                pickupMins + " mins", vehicleType
            ));
        }

        return riders;
    }

    public DeliveryAssignment getOptimalAssignment(String orderId) {
        List<RiderLocation> riders = findNearestRiders(orderId, 5, 10.0);

        if (riders.isEmpty()) {
            return new DeliveryAssignment(orderId, null, new ArrayList<>(), 0, "No riders available");
        }

        RiderLocation best = riders.get(0);
        
        double avgSpeedKmh = 25;
        double totalTime = (best.distanceKm / avgSpeedKmh) * 60;

        String routeRec = "Optimal route: Pick from " + best.distanceKm + 
                        "km away, deliver in " + Math.round(totalTime) + " mins";

        return new DeliveryAssignment(orderId, best, riders, totalTime, routeRec);
    }

    public Map<String, Object> getDeliveryDashboard(String warehouseId) {
        Map<String, Object> dashboard = new HashMap<>();

        Query available = em.createNativeQuery(
            "SELECT COUNT(*) FROM delivery_agents " +
            "WHERE warehouse_id = ?1 AND status = 'AVAILABLE'"
        );
        available.setParameter(1, warehouseId);
        dashboard.put("availableRiders", ((Number) available.getSingleResult()).intValue());

        Query busy = em.createNativeQuery(
            "SELECT COUNT(*) FROM delivery_agents " +
            "WHERE warehouse_id = ?1 AND status IN ('BUSY','ON_DELIVERY')"
        );
        busy.setParameter(1, warehouseId);
        dashboard.put("busyRiders", ((Number) busy.getSingleResult()).intValue());

        Query activeOrders = em.createNativeQuery(
            "SELECT COUNT(*) FROM orders " +
            "WHERE status IN ('OUT_FOR_DELIVERY') AND assigned_rider IS NOT NULL"
        );
        dashboard.put("activeDeliveries", ((Number) activeOrders.getSingleResult()).intValue());

        Query pendingOrders = em.createNativeQuery(
            "SELECT COUNT(*) FROM orders " +
            "WHERE status = 'PACKING' AND assigned_rider IS NULL"
        );
        dashboard.put("pendingAssignment", ((Number) pendingOrders.getSingleResult()).intValue());

        Query avgDelivery = em.createNativeQuery(
            "SELECT AVG(distance_km) FROM orders " +
            "WHERE status = 'DELIVERED' AND created_at > CURRENT_DATE - INTERVAL '7' DAY"
        );
        Number avg = (Number) avgDelivery.getSingleResult();
        dashboard.put("avgDeliveryKm", avg != null ? Math.round(avg.doubleValue() * 10) / 10.0 : 0);

        return dashboard;
    }

    @Transactional
    public boolean assignRiderToOrder(String orderId, String riderId) {
        int updated = em.createNativeQuery(
            "UPDATE orders SET assigned_rider = ?1, status = 'OUT_FOR_DELIVERY' " +
            "WHERE id = ?2 AND assigned_rider IS NULL"
        ).setParameter(1, riderId).setParameter(2, orderId).executeUpdate();

        if (updated > 0) {
            em.createNativeQuery(
                "UPDATE delivery_agents SET status = 'ON_DELIVERY', today_orders = today_orders + 1 " +
                "WHERE id = ?1"
            ).setParameter(1, riderId).executeUpdate();
            return true;
        }
        return false;
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double earthRadius = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                  Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                  Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }
}