package com.supplychain.util;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

public class SubstitutionEngine {

    public static class ProductInfo {
        private final String productId;
        private final String name;
        private final String category;
        private final BigDecimal price;
        private final BigDecimal weight;
        private final Set<String> tags;

        public ProductInfo(String productId, String name, String category,
                        BigDecimal price, BigDecimal weight, Set<String> tags) {
            this.productId = productId;
            this.name = name;
            this.category = category;
            this.price = price;
            this.weight = weight;
            this.tags = tags != null ? new HashSet<>(tags) : new HashSet<>();
        }

        public String getProductId() { return productId; }
        public String getName() { return name; }
        public String getCategory() { return category; }
        public BigDecimal getPrice() { return price; }
        public BigDecimal getWeight() { return weight; }
        public Set<String> getTags() { return tags; }
    }

    public static class SubstitutionRecommendation {
        private final String originalProductId;
        private final String originalProductName;
        private final String substituteProductId;
        private final String substituteProductName;
        private final double similarityScore;
        private final String matchReason;
        private final BigDecimal priceDifference;
        private final boolean isAvailable;

        public SubstitutionRecommendation(String originalProductId, String originalProductName,
                                       String substituteProductId, String substituteProductName,
                                       double similarityScore, String matchReason,
                                       BigDecimal priceDifference, boolean isAvailable) {
            this.originalProductId = originalProductId;
            this.originalProductName = originalProductName;
            this.substituteProductId = substituteProductId;
            this.substituteProductName = substituteProductName;
            this.similarityScore = similarityScore;
            this.matchReason = matchReason;
            this.priceDifference = priceDifference;
            this.isAvailable = isAvailable;
        }

        public String getOriginalProductId() { return originalProductId; }
        public String getOriginalProductName() { return originalProductName; }
        public String getSubstituteProductId() { return substituteProductId; }
        public String getSubstituteProductName() { return substituteProductName; }
        public double getSimilarityScore() { return similarityScore; }
        public String getMatchReason() { return matchReason; }
        public BigDecimal getPriceDifference() { return priceDifference; }
        public boolean isAvailable() { return isAvailable; }
    }

    public static class SubstitutionResult {
        private final String requestedProductId;
        private final boolean isAvailable;
        private final List<SubstitutionRecommendation> recommendations;
        private final String primaryRecommendation;
        private final String message;

        public SubstitutionResult(String requestedProductId, boolean isAvailable,
                                List<SubstitutionRecommendation> recommendations,
                                String primaryRecommendation, String message) {
            this.requestedProductId = requestedProductId;
            this.isAvailable = isAvailable;
            this.recommendations = recommendations;
            this.primaryRecommendation = primaryRecommendation;
            this.message = message;
        }

        public String getRequestedProductId() { return requestedProductId; }
        public boolean isAvailable() { return isAvailable; }
        public List<SubstitutionRecommendation> getRecommendations() { return recommendations; }
        public String getPrimaryRecommendation() { return primaryRecommendation; }
        public String getMessage() { return message; }
    }

    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.5;

    private final Map<String, ProductInfo> productCatalog;
    private final Map<String, List<String>> categoryProducts;
    private final Map<String, List<SubstitutionRule>> substitutionRules;
    private final Map<String, List<String>> manualSubstitutions;

    public static class SubstitutionRule {
        private final String productId1;
        private final String productId2;
        private final double score;
        private final String reason;

        public SubstitutionRule(String productId1, String productId2, double score, String reason) {
            this.productId1 = productId1;
            this.productId2 = productId2;
            this.score = score;
            this.reason = reason;
        }

        public String getProductId1() { return productId1; }
        public String getProductId2() { return productId2; }
        public double getScore() { return score; }
        public String getReason() { return reason; }
    }

    public SubstitutionEngine() {
        this.productCatalog = new HashMap<>();
        this.categoryProducts = new HashMap<>();
        this.substitutionRules = new HashMap<>();
        this.manualSubstitutions = new HashMap<>();
    }

    public void registerProduct(String productId, String name, String category,
                             BigDecimal price, BigDecimal weight, String... tags) {
        Set<String> tagSet = new HashSet<>(Arrays.asList(tags));
        ProductInfo info = new ProductInfo(productId, name, category, price, weight, tagSet);
        productCatalog.put(productId, info);

        categoryProducts.computeIfAbsent(category, k -> new ArrayList<>()).add(productId);
    }

    public void registerProduct(ProductInfo product) {
        productCatalog.put(product.getProductId(), product);
        categoryProducts.computeIfAbsent(product.getCategory(), k -> new ArrayList<>())
            .add(product.getProductId());
    }

    public void addManualSubstitution(String productId, String substituteId) {
        manualSubstitutions.computeIfAbsent(productId, k -> new ArrayList<>()).add(substituteId);
    }

    public void addSubstitutionRule(String productId1, String productId2, double score, String reason) {
        substitutionRules.computeIfAbsent(productId1, k -> new ArrayList<>())
            .add(new SubstitutionRule(productId1, productId2, score, reason));
    }

    public double calculateSimilarity(ProductInfo p1, ProductInfo p2) {
        if (p1 == null || p2 == null) return 0.0;

        double categoryScore = p1.getCategory().equals(p2.getCategory()) ? 1.0 : 0.0;

        double priceDiff = Math.abs(
            p1.getPrice().subtract(p2.getPrice()).doubleValue()
        );
        double maxPrice = Math.max(p1.getPrice().doubleValue(), p2.getPrice().doubleValue());
        double priceScore = maxPrice > 0 ? Math.max(0, 1 - (priceDiff / maxPrice)) : 1.0;

        double weightDiff = Math.abs(
            p1.getWeight().subtract(p2.getWeight()).doubleValue()
        );
        double maxWeight = Math.max(p1.getWeight().doubleValue(), p2.getWeight().doubleValue());
        double weightScore = maxWeight > 0 ? Math.max(0, 1 - (weightDiff / maxWeight)) : 1.0;

        Set<String> tags1 = p1.getTags();
        Set<String> tags2 = p2.getTags();
        Set<String> intersection = new HashSet<>(tags1);
        intersection.retainAll(tags2);
        Set<String> union = new HashSet<>(tags1);
        union.addAll(tags2);
        double tagScore = union.isEmpty() ? 0.0 :
            (double) intersection.size() / union.size();

        return (categoryScore * 0.4) + (priceScore * 0.2) + (weightScore * 0.2) + (tagScore * 0.2);
    }

    public List<SubstitutionRecommendation> findSubstitutes(String productId, Set<String> outOfStockProducts) {
        ProductInfo original = productCatalog.get(productId);
        if (original == null) return new ArrayList<>();

        List<SubstitutionRecommendation> recommendations = new ArrayList<>();

        List<String> manualSubs = manualSubstitutions.getOrDefault(productId, new ArrayList<>());
        for (String subId : manualSubs) {
            ProductInfo sub = productCatalog.get(subId);
            if (sub != null) {
                boolean available = !outOfStockProducts.contains(subId);
                recommendations.add(new SubstitutionRecommendation(
                    productId, original.getName(),
                    subId, sub.getName(),
                    1.0, "Manual substitution",
                    sub.getPrice().subtract(original.getPrice()),
                    available
                ));
            }
        }

        List<SubstitutionRule> rules = substitutionRules.getOrDefault(productId, new ArrayList<>());
        for (SubstitutionRule rule : rules) {
            ProductInfo sub = productCatalog.get(rule.getProductId2());
            if (sub != null) {
                boolean available = !outOfStockProducts.contains(rule.getProductId2());
                recommendations.add(new SubstitutionRecommendation(
                    productId, original.getName(),
                    rule.getProductId2(), sub.getName(),
                    rule.getScore(), rule.getReason(),
                    sub.getPrice().subtract(original.getPrice()),
                    available
                ));
            }
        }

        String category = original.getCategory();
        List<String> sameCategory = categoryProducts.getOrDefault(category, new ArrayList<>());

        for (String candidateId : sameCategory) {
            if (candidateId.equals(productId)) continue;
            if (outOfStockProducts.contains(candidateId)) continue;
            if (recommendations.stream().anyMatch(r -> r.getSubstituteProductId().equals(candidateId))) {
                continue;
            }

            ProductInfo candidate = productCatalog.get(candidateId);
            double simScore = calculateSimilarity(original, candidate);
            if (simScore >= DEFAULT_SIMILARITY_THRESHOLD) {
                String reason = determineMatchReason(original, candidate);
                recommendations.add(new SubstitutionRecommendation(
                    productId, original.getName(),
                    candidateId, candidate.getName(),
                    simScore, reason,
                    candidate.getPrice().subtract(original.getPrice()),
                    true
                ));
            }
        }

        return recommendations.stream()
            .sorted(Comparator.comparingDouble(SubstitutionRecommendation::getSimilarityScore).reversed())
            .collect(Collectors.toList());
    }

    private String determineMatchReason(ProductInfo original, ProductInfo candidate) {
        List<String> reasons = new ArrayList<>();

        if (original.getCategory().equals(candidate.getCategory())) {
            reasons.add("Same category");
        }

        double priceDiff = Math.abs(
            original.getPrice().subtract(candidate.getPrice()).doubleValue()
        );
        if (priceDiff <= 5) {
            reasons.add("Similar price");
        }

        double weightDiff = Math.abs(
            original.getWeight().subtract(candidate.getWeight()).doubleValue()
        );
        if (weightDiff <= 0.1) {
            reasons.add("Similar weight");
        }

        Set<String> commonTags = new HashSet<>(original.getTags());
        commonTags.retainAll(candidate.getTags());
        if (!commonTags.isEmpty()) {
            reasons.add("Shared: " + String.join(", ", commonTags));
        }

        return reasons.isEmpty() ? "Similar product" : String.join(", ", reasons);
    }

    public SubstitutionResult getRecommendation(String productId, Set<String> outOfStockProducts) {
        boolean isAvailable = !outOfStockProducts.contains(productId);

        if (isAvailable) {
            return new SubstitutionResult(
                productId, true, new ArrayList<>(), null,
                "Product is available"
            );
        }

        List<SubstitutionRecommendation> subs = findSubstitutes(productId, outOfStockProducts);

        List<SubstitutionRecommendation> availableSubs = subs.stream()
            .filter(SubstitutionRecommendation::isAvailable)
            .collect(Collectors.toList());

        String primary = availableSubs.isEmpty() ? null : availableSubs.get(0).getSubstituteProductId();

        String message;
        if (availableSubs.isEmpty()) {
            message = "No substitutes available";
        } else {
            message = String.format("Found %d substitute(s). Best match: %s (%.1f%% similar)",
                availableSubs.size(), primary,
                availableSubs.get(0).getSimilarityScore() * 100);
        }

        return new SubstitutionResult(productId, false, subs, primary, message);
    }

    public SubstitutionResult getRecommendation(String productId) {
        return getRecommendation(productId, new HashSet<>());
    }

    public List<SubstitutionRecommendation> getAllRecommendations(Set<String> outOfStockProducts) {
        List<SubstitutionRecommendation> all = new ArrayList<>();

        for (String productId : outOfStockProducts) {
            all.addAll(findSubstitutes(productId, outOfStockProducts));
        }

        return all.stream()
            .sorted(Comparator.comparingDouble(SubstitutionRecommendation::getSimilarityScore).reversed())
            .collect(Collectors.toList());
    }

    public String generateSubstitutionReport(Set<String> outOfStockProducts) {
        StringBuilder report = new StringBuilder();
        report.append("=== SUBSTITUTION REPORT ===\n\n");

        for (String productId : outOfStockProducts) {
            ProductInfo original = productCatalog.get(productId);
            if (original == null) continue;

            List<SubstitutionRecommendation> subs = findSubstitutes(productId, outOfStockProducts);
            List<SubstitutionRecommendation> available = subs.stream()
                .filter(SubstitutionRecommendation::isAvailable)
                .collect(Collectors.toList());

            report.append(String.format("Product: %s (%s)\n", original.getName(), productId));
            report.append(String.format("  Substitutes available: %d\n", available.size()));

            if (!available.isEmpty()) {
                report.append("  Top recommendation:\n");
                SubstitutionRecommendation top = available.get(0);
                report.append(String.format("    -> %s (%s)\n      Similarity: %.1f%%\n      Reason: %s\n",
                    top.getSubstituteProductName(), top.getSubstituteProductId(),
                    top.getSimilarityScore() * 100, top.getMatchReason()));
            }
            report.append("\n");
        }

        return report.toString();
    }

    public int getProductCount() {
        return productCatalog.size();
    }

    public Set<String> getCategories() {
        return categoryProducts.keySet();
    }

    public ProductInfo getProductInfo(String productId) {
        return productCatalog.get(productId);
    }

    public void clear() {
        productCatalog.clear();
        categoryProducts.clear();
        substitutionRules.clear();
        manualSubstitutions.clear();
    }
}