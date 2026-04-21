package com.supplychain.util;

import java.util.*;

public class KMeansClustering {

    public static class Point {
        public String id;
        public float x;
        public float y;
        public String orderId;

        public Point(String id, float x, float y) {
            this.id = id;
            this.x = x;
            this.y = y;
        }
    }

    public static class Cluster {
        public int id;
        public Point centroid;
        public List<Point> points = new ArrayList<>();
        public List<String> orderIds = new ArrayList<>();
    }

    public static List<Cluster> cluster(List<Point> points, int k, int maxIter) {
        if (points.isEmpty() || k <= 0) return new ArrayList<>();
        k = Math.min(k, points.size());

        List<Point> centroids = initKMeansPlusPlus(points, k);
        int[] assignments = new int[points.size()];
        boolean changed = true;
        int iter = 0;

        while (changed && iter < maxIter) {
            changed = false;
            iter++;

            for (int i = 0; i < points.size(); i++) {
                double minDist = Double.MAX_VALUE;
                int cluster = 0;
                for (int c = 0; c < centroids.size(); c++) {
                    double d = euclidean(points.get(i), centroids.get(c));
                    if (d < minDist) {
                        minDist = d;
                        cluster = c;
                    }
                }
                if (assignments[i] != cluster) {
                    assignments[i] = cluster;
                    changed = true;
                }
            }

            for (int c = 0; c < centroids.size(); c++) {
                List<Point> clusterPoints = new ArrayList<>();
                for (int i = 0; i < points.size(); i++) {
                    if (assignments[i] == c) {
                        clusterPoints.add(points.get(i));
                    }
                }
                if (!clusterPoints.isEmpty()) {
                    float sumX = 0, sumY = 0;
                    for (Point p : clusterPoints) {
                        sumX += p.x;
                        sumY += p.y;
                    }
                    centroids.set(c, new Point(centroids.get(c).id,
                        sumX / clusterPoints.size(),
                        sumY / clusterPoints.size()));
                }
            }
        }

        List<Cluster> clusters = new ArrayList<>();
        for (int c = 0; c < k; c++) {
            Cluster cluster = new Cluster();
            cluster.id = c;
            cluster.centroid = centroids.get(c);
            for (int i = 0; i < points.size(); i++) {
                if (assignments[i] == c) {
                    cluster.points.add(points.get(i));
                    if (points.get(i).orderId != null) {
                        cluster.orderIds.add(points.get(i).orderId);
                    }
                }
            }
            clusters.add(cluster);
        }

        clusters.removeIf(c -> c.points.isEmpty());
        return clusters;
    }

    private static List<Point> initKMeansPlusPlus(List<Point> points, int k) {
        List<Point> centroids = new ArrayList<>();
        Random rand = new Random();
        centroids.add(points.get(rand.nextInt(points.size())));

        for (int i = 1; i < k; i++) {
            double[] distances = new double[points.size()];
            double minD;
            double total = 0;
            for (int j = 0; j < points.size(); j++) {
                minD = Double.MAX_VALUE;
                for (Point c : centroids) {
                    double d = euclidean(points.get(j), c);
                    if (d < minD) minD = d;
                }
                distances[j] = minD * minD;
                total += distances[j];
            }

            double randVal = rand.nextDouble() * total;
            for (int j = 0; j < points.size(); j++) {
                randVal -= distances[j];
                if (randVal <= 0) {
                    centroids.add(points.get(j));
                    break;
                }
            }
            if (centroids.size() <= i) {
                centroids.add(points.get(rand.nextInt(points.size())));
            }
        }
        return centroids;
    }

    public static double euclidean(Point a, Point b) {
        double dx = a.x - b.x;
        double dy = a.y - b.y;
        return Math.sqrt(dx * dx + dy * dy);
    }
}