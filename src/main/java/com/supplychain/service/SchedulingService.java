package com.supplychain.service;

import com.supplychain.model.Order;
import com.supplychain.model.enums.Priority;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class SchedulingService {

    private static final Map<Priority, Integer> PRIORITY_WEIGHT = Map.of(
        Priority.HIGH, 0,
        Priority.MEDIUM, 10,
        Priority.LOW, 20
    );

    public static class ScheduledOrder {
        public String orderId;
        public int score;
        public Order order;

        public ScheduledOrder(String orderId, int score, Order order) {
            this.orderId = orderId;
            this.score = score;
            this.order = order;
        }

        public int compareTo(ScheduledOrder other) {
            return Integer.compare(this.score, other.score);
        }
    }

    public static class DeliveryScheduler {
        private final PriorityQueue<ScheduledOrder> queue = new PriorityQueue<>(
            Comparator.comparingInt(o -> o.score));

        public void addOrder(Order order) {
            int score = calculateScore(order);
            queue.add(new ScheduledOrder(order.getId(), score, order));
        }

        public Order nextOrder() {
            ScheduledOrder scheduled = queue.poll();
            return scheduled != null ? scheduled.order : null;
        }

        public Order peekOrder() {
            ScheduledOrder scheduled = queue.peek();
            return scheduled != null ? scheduled.order : null;
        }

        public List<Order> getSchedule() {
            List<ScheduledOrder> all = new ArrayList<>(queue);
            all.sort(Comparator.comparingInt(o -> o.score));
            List<Order> result = new ArrayList<>();
            for (int i = 0; i < all.size(); i++) {
                Order order = all.get(i).order;
                result.add(order);
            }
            return result;
        }

        public void reschedule(List<Order> orders) {
            queue.clear();
            for (Order order : orders) {
                addOrder(order);
            }
        }

        public int size() {
            return queue.size();
        }

        public boolean isEmpty() {
            return queue.isEmpty();
        }
    }

    public static int calculateScore(Order order) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = order.getDeadline();

        if (deadline == null) {
            deadline = now.plusDays(3);
        }

        long hoursLeft = ChronoUnit.HOURS.between(now, deadline);
        hoursLeft = Math.max(0, hoursLeft);

        int priorityPenalty = PRIORITY_WEIGHT.getOrDefault(order.getPriority(), 10);

        return (int) hoursLeft + priorityPenalty;
    }

    public DeliveryScheduler createScheduler() {
        return new DeliveryScheduler();
    }
}