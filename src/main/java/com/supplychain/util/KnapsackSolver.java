package com.supplychain.util;

import java.util.*;

public class KnapsackSolver {

    public static class KnapsackItem {
        public String id;
        public int weight;
        public int volume;
        public int value;

        public KnapsackItem(String id, int weight, int volume, int value) {
            this.id = id;
            this.weight = weight;
            this.volume = volume;
            this.value = value;
        }
    }

    public static class Result {
        public int maxValue;
        public List<KnapsackItem> selectedItems;
        public int usedWeight;
        public int usedVolume;
        public int remainingWeight;
        public int remainingVolume;
        public int efficiency;
    }

    public static Result solve01(List<KnapsackItem> items, int weightCapacity, int volumeCapacity) {
        int n = items.size();
        int W = weightCapacity;
        int V = volumeCapacity;

        int[][][] dp = new int[n + 1][W + 1][V + 1];

        for (int i = 1; i <= n; i++) {
            KnapsackItem item = items.get(i - 1);
            for (int w = 0; w <= W; w++) {
                for (int v = 0; v <= V; v++) {
                    dp[i][w][v] = dp[i - 1][w][v];
                    if (item.weight <= w && item.volume <= v) {
                        int val = dp[i - 1][w - item.weight][v - item.volume] + item.value;
                        if (val > dp[i][w][v]) {
                            dp[i][w][v] = val;
                        }
                    }
                }
            }
        }

        Result result = new Result();
        result.maxValue = dp[n][W][V];
        result.selectedItems = new ArrayList<>();

        int w = W, v = V;
        for (int i = n; i > 0 && w > 0 && v > 0; i--) {
            if (dp[i][w][v] != dp[i - 1][w][v]) {
                KnapsackItem item = items.get(i - 1);
                result.selectedItems.add(item);
                w = w - item.weight;
                v = v - item.volume;
            }
        }

        result.usedWeight = W - w;
        result.usedVolume = V - v;
        result.remainingWeight = w;
        result.remainingVolume = v;
        result.efficiency = n > 0 ? Math.round((float) result.selectedItems.size() / n * 100) : 0;

        return result;
    }

    public static class FractionalResult {
        public int totalValue;
        public List<KnapsackItem> taken;
        public int remaining;
    }

    public static FractionalResult solveFractional(List<KnapsackItem> items, int capacity) {
        FractionalResult result = new FractionalResult();
        result.taken = new ArrayList<>();

        List<KnapsackItem> sorted = new ArrayList<>(items);
        sorted.sort((a, b) -> Float.compare((float) b.value / b.weight, (float) a.value / a.weight));

        int remaining = capacity;
        for (KnapsackItem item : sorted) {
            if (remaining <= 0) break;
            if (item.weight <= remaining) {
                result.taken.add(item);
                result.totalValue += item.value;
                remaining = remaining - item.weight;
            } else {
                double fraction = (double) remaining / item.weight;
                int partialVolume = (int) Math.ceil(item.volume * fraction);
                int partialValue = (int) Math.ceil(item.value * fraction);
                KnapsackItem partial = new KnapsackItem(item.id, remaining, partialVolume, partialValue);
                result.taken.add(partial);
                result.totalValue = result.totalValue + partialValue;
                remaining = 0;
            }
        }
        result.remaining = remaining;

        return result;
    }
}