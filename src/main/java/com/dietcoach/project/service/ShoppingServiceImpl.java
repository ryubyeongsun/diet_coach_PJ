package com.dietcoach.project.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dietcoach.project.client.shopping.ShoppingClient;
import com.dietcoach.project.client.shopping.ShoppingClientResult;
import com.dietcoach.project.domain.ShoppingProduct;
import com.dietcoach.project.dto.ShoppingProductResponse;
import com.dietcoach.project.dto.ShoppingProductsResponse;
import com.dietcoach.project.dto.meal.ShoppingListResponse;
import com.dietcoach.project.util.shopping.IngredientQueryNormalizer;
import com.dietcoach.project.service.shopping.AiProductReranker;
import com.dietcoach.project.service.shopping.ProductScorer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShoppingServiceImpl implements ShoppingService {

    private final ShoppingClient shoppingClient;
    private final ShoppingCategoryService categoryService;
    private final IngredientQueryNormalizer queryNormalizer;
    private final ProductScorer productScorer;
    private final AiProductReranker aiReranker;
    private final java.util.concurrent.ConcurrentHashMap<String, Integer> pricePer100gCache = new java.util.concurrent.ConcurrentHashMap<>();
    private static final int PRICE_PER_100G_TTL_MIN = 60;
    private final java.util.concurrent.ConcurrentHashMap<String, Long> pricePer100gExpire = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    @Transactional(readOnly = true)
    public ShoppingProductsResponse search(String keyword, int page, int size) {
        ShoppingClientResult result = shoppingClient.searchProducts(keyword, page, size);

        List<ShoppingProduct> filteredProducts = filterProducts(result.getProducts());
        updatePriceStats(keyword, filteredProducts);
        List<ShoppingProductResponse> mapped = (filteredProducts == null)
                ? List.of()
                : filteredProducts.stream().map(this::toResponse).toList();

        return ShoppingProductsResponse.builder()
                .source(result.getSource())
                .products(mapped)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SearchOneResult searchOne(String keyword, int allocatedBudget, Long totalGram) {
        String ingredientName = (keyword == null) ? "" : keyword.trim();
        int topN = 20; // Expanded to 20
        String source = "REAL";

        // 1. Normalize Query
        String normalizedQuery = queryNormalizer.normalize(ingredientName);
        if (normalizedQuery.isBlank()) {
            normalizedQuery = ingredientName; // Fallback
        }
        
        // PRD 4-1: Suffix "식품" to filter non-food items
        // Check if it already has "식품" or similar
        if (!containsAny(normalizedQuery, "식품", "식재료")) {
            normalizedQuery += " 식품";
        }
        
        // 2. Target Category
        String targetCatNo = categoryService.findBestCategory(ingredientName).orElse(null);

        // 3. Search (TopN)
        ShoppingClientResult result = shoppingClient.searchProducts(normalizedQuery, 1, topN, targetCatNo);
        List<ShoppingProduct> products = (result == null || result.getProducts() == null)
                ? List.of()
                : result.getProducts();
        products = filterProducts(products);
        updatePriceStats(ingredientName, products);
        source = (result == null || result.getSource() == null) ? "NONE" : result.getSource();

        if (products.isEmpty()) {
            ShoppingClientResult relaxed = shoppingClient.searchProducts(normalizedQuery, 1, topN, null);
            List<ShoppingProduct> relaxedProducts = (relaxed == null || relaxed.getProducts() == null)
                    ? List.of()
                    : relaxed.getProducts();
            products = filterProductsRelaxed(relaxedProducts);
            updatePriceStats(ingredientName, products);
            source = (relaxed == null || relaxed.getSource() == null) ? source : relaxed.getSource();
        }

        if (products.isEmpty()) {
            ShoppingClientResult fallback = shoppingClient.searchProducts(ingredientName, 1, topN, null);
            List<ShoppingProduct> fallbackProducts = (fallback == null || fallback.getProducts() == null)
                    ? List.of()
                    : fallback.getProducts();
            products = filterProductsRelaxed(fallbackProducts);
            updatePriceStats(ingredientName, products);
            source = (fallback == null || fallback.getSource() == null) ? source : fallback.getSource();
        }

        log.info("[SHOPPING_LIST][{}] SEARCH_REQ ingredient={} query=\"{}\" cat={} limit={} source={} allocated={} gram={}",
                currentTraceId(), ingredientName, normalizedQuery, targetCatNo, topN, source, allocatedBudget, totalGram);
        
        // Log Top 5 candidates for debugging
        logSearchTopN(ingredientName, products, 5);

        // 4. Score & Filter (Top 10) for AI
        List<ShoppingProduct> topCandidates = productScorer.selectTopN(products, ingredientName, allocatedBudget, totalGram, 10);
        if (topCandidates.isEmpty() && !products.isEmpty()) {
            topCandidates = products.stream().limit(10).toList();
        }
        
        ShoppingProduct best = null;
        String reason = "NO_MATCH";

        if (topCandidates.size() >= 3) {
            // 5. AI Rerank (Parallel-Ready)
            // AI Rerank might need totalGram context too, but keeping it simple for now
            try {
                ShoppingProduct aiSelected = aiReranker.rerank(ingredientName, allocatedBudget, topCandidates);
                if (aiSelected != null) {
                    best = aiSelected;
                    reason = "AI_SELECTED";
                } else {
                    // Fallback to top scorer
                    best = topCandidates.get(0);
                    reason = "AI_FAIL_FALLBACK";
                }
            } catch (Exception e) {
                 best = topCandidates.get(0);
                 reason = "AI_ERROR_FALLBACK";
            }
        } else if (!topCandidates.isEmpty()) {
            best = topCandidates.get(0);
            reason = "RULE_TOP1";
        }
        BudgetGuardResult guarded = enforceBudgetFit(ingredientName, allocatedBudget, totalGram, topCandidates, best, reason);
        best = guarded.product();
        reason = guarded.reason();
        
        ShoppingListResponse.ProductCard card = toCard(best);
        
        logSelected(ingredientName, best, card, reason, allocatedBudget);

        return new SearchOneResult(card, source);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getEstimatedCostPer100g(String ingredientName) {
        String key = normalizeKey(ingredientName);
        if (key.isEmpty()) return null;
        Long exp = pricePer100gExpire.get(key);
        if (exp != null && exp < System.currentTimeMillis()) {
            pricePer100gExpire.remove(key);
            pricePer100gCache.remove(key);
            return null;
        }
        return pricePer100gCache.get(key);
    }

    private ShoppingProductResponse toResponse(ShoppingProduct p) {
        return ShoppingProductResponse.builder()
                .externalId(p.getExternalId())
                .title(p.getTitle())
                .price(p.getPrice())
                .gramPerUnit(p.getGramPerUnit())
                .imageUrl(p.getImageUrl())
                .productUrl(p.getProductUrl())
                .mallName(p.getMallName())
                .build();
    }

    private void logSearchTopN(String ingredientName, List<ShoppingProduct> products, int topN) {
        String traceId = currentTraceId();
        if (products == null || products.isEmpty()) {
            log.info("[SHOPPING_LIST][{}] SEARCH_TOP{} ingredient={} empty=true",
                    traceId, topN, ingredientName);
            return;
        }

        int limit = Math.min(topN, products.size());
        StringBuilder summary = new StringBuilder();
        for (int i = 0; i < limit; i++) {
            ShoppingProduct p = products.get(i);
            if (i > 0) {
                summary.append(", ");
            }
            summary.append("rank=").append(i + 1)
                    .append(" name=\"").append(truncate(p.getTitle(), 30)).append("\"")
                    .append(" price=").append(p.getPrice());
        }

        log.info("[SHOPPING_LIST][{}] SEARCH_TOP{} ingredient={} {}",
                traceId, topN, ingredientName, summary);
    }

    private void logSelected(String ingredientName, ShoppingProduct product, ShoppingListResponse.ProductCard card,
            String reason, int allocatedBudget) {
        String traceId = currentTraceId();
        if (product == null || card == null) {
            log.info("[SHOPPING_LIST][{}] SELECTED ingredient={} budget={} selected=null reason={}",
                    traceId, ingredientName, allocatedBudget, reason);
            return;
        }
        log.info("[SHOPPING_LIST][{}] SELECTED ingredient={} budget={} selected=\"{}\" price={} reason={}",
                traceId,
                ingredientName,
                allocatedBudget,
                truncate(card.getProductName(), 40),
                card.getPrice(),
                reason);
    }

    private String currentTraceId() {
        String traceId = MDC.get("traceId");
        return (traceId == null || traceId.isBlank()) ? "no-trace" : traceId;
    }

    private ShoppingListResponse.ProductCard toCard(ShoppingProduct product) {
        if (product == null) {
            return null;
        }
        return ShoppingListResponse.ProductCard.builder()
                .productName(product.getTitle())
                .price(Long.valueOf(product.getPrice()))
                .imageUrl(product.getImageUrl())
                .productUrl(product.getProductUrl())
                .packageGram((product.getGramPerUnit() != null) ? product.getGramPerUnit().intValue() : 0)
                .build();
    }

    private String truncate(String value, int max) {
        if (value == null) return null;
        if (value.length() <= max) return value;
        return value.substring(0, max);
    }

    private record BudgetGuardResult(ShoppingProduct product, String reason) {}

    private BudgetGuardResult enforceBudgetFit(
            String ingredientName,
            int allocatedBudget,
            Long totalGram,
            List<ShoppingProduct> candidates,
            ShoppingProduct selected,
            String reason
    ) {
        if (allocatedBudget <= 0 || candidates == null || candidates.isEmpty()) {
            return new BudgetGuardResult(selected, reason);
        }
        if (selected != null && isWithinBudget(selected, allocatedBudget, totalGram, 1.25)) {
            return new BudgetGuardResult(selected, reason);
        }

        List<ShoppingProduct> withinBudget = candidates.stream()
                .filter(p -> p != null && p.getPrice() > 0)
                .filter(p -> isWithinBudget(p, allocatedBudget, totalGram, 1.25))
                .toList();

        if (!withinBudget.isEmpty()) {
            ShoppingProduct closest = withinBudget.stream()
                    .sorted(Comparator.comparingDouble(p -> budgetDistance(p, allocatedBudget, totalGram)))
                    .findFirst()
                    .orElse(null);
            if (closest != null) {
                log.info("[SHOPPING_LIST][{}] BUDGET_GUARD ingredient={} selected=\"{}\" reason={} allocated={}",
                        currentTraceId(), ingredientName, truncate(closest.getTitle(), 40), reason, allocatedBudget);
                return new BudgetGuardResult(closest, "BUDGET_GUARD");
            }
        }
        ShoppingProduct cheapestPerGram = candidates.stream()
                .filter(p -> p != null && p.getPrice() > 0)
                .sorted(Comparator.comparingDouble(p -> pricePerGram(p, totalGram)))
                .findFirst()
                .orElse(null);

        if (cheapestPerGram != null) {
            log.info("[SHOPPING_LIST][{}] BUDGET_GUARD ingredient={} selected=\"{}\" reason={} allocated={}",
                    currentTraceId(), ingredientName, truncate(cheapestPerGram.getTitle(), 40), reason, allocatedBudget);
            return new BudgetGuardResult(cheapestPerGram, "BUDGET_GUARD");
        }

        return new BudgetGuardResult(selected, reason);
    }

    private boolean isWithinBudget(ShoppingProduct product, int budget, Long totalGram, double tolerance) {
        if (product == null || product.getPrice() <= 0) return false;
        long projected = projectedTotalCost(product, totalGram);
        return projected <= Math.round(budget * tolerance);
    }

    private long projectedTotalCost(ShoppingProduct product, Long totalGram) {
        if (product == null || product.getPrice() <= 0) return Long.MAX_VALUE;
        if (totalGram != null && totalGram > 0 && product.getGramPerUnit() != null && product.getGramPerUnit() > 0) {
            int count = (int) Math.ceil((double) totalGram / product.getGramPerUnit());
            return (long) product.getPrice() * count;
        }
        return product.getPrice();
    }

    private double pricePerGram(ShoppingProduct product, Long totalGram) {
        if (product == null || product.getPrice() <= 0) return Double.MAX_VALUE;
        if (product.getGramPerUnit() != null && product.getGramPerUnit() > 0) {
            return product.getPrice() / product.getGramPerUnit();
        }
        long projected = projectedTotalCost(product, totalGram);
        if (totalGram != null && totalGram > 0) {
            return (double) projected / totalGram;
        }
        return product.getPrice();
    }

    private double budgetDistance(ShoppingProduct product, int budget, Long totalGram) {
        if (budget <= 0) return Double.MAX_VALUE;
        long projected = projectedTotalCost(product, totalGram);
        return Math.abs(projected - budget);
    }

    private List<ShoppingProduct> filterProducts(List<ShoppingProduct> products) {
        if (products == null) return List.of();
        return products.stream()
                .filter(p -> p != null && categoryService.isValidProductTitle(p.getTitle()))
                .filter(p -> categoryService.isValidCategory(p.getCategoryCode(), p.getCategoryName()))
                .toList();
    }

    private List<ShoppingProduct> filterProductsRelaxed(List<ShoppingProduct> products) {
        if (products == null) return List.of();
        return products.stream()
                .filter(p -> p != null && categoryService.isValidProductTitle(p.getTitle()))
                .toList();
    }

    private boolean containsAny(String text, String... tokens) {
        if (text == null) return false;
        String normalized = text.toLowerCase(Locale.ROOT);
        for (String token : tokens) {
            if (token == null) continue;
            if (normalized.contains(token.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    private void updatePriceStats(String ingredientName, List<ShoppingProduct> products) {
        if (products == null || products.isEmpty()) return;
        String key = normalizeKey(ingredientName);
        if (key.isEmpty()) return;

        List<Double> per100g = products.stream()
                .filter(p -> p != null && p.getPrice() > 0 && p.getGramPerUnit() != null && p.getGramPerUnit() > 0)
                .map(p -> (p.getPrice() / p.getGramPerUnit()) * 100.0)
                .filter(v -> v > 0 && v < 100000)
                .sorted()
                .toList();

        if (per100g.isEmpty()) return;
        double median = per100g.get(per100g.size() / 2);
        int rounded = (int) Math.round(median);
        pricePer100gCache.put(key, rounded);
        pricePer100gExpire.put(key, System.currentTimeMillis() + PRICE_PER_100G_TTL_MIN * 60_000L);
    }

    private String normalizeKey(String value) {
        if (value == null) return "";
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
