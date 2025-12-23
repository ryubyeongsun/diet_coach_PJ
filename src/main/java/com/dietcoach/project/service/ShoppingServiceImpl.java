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

    @Override
    @Transactional(readOnly = true)
    public ShoppingProductsResponse search(String keyword, int page, int size) {
        ShoppingClientResult result = shoppingClient.searchProducts(keyword, page, size);

        List<ShoppingProductResponse> mapped = (result.getProducts() == null)
                ? List.of()
                : result.getProducts().stream().map(this::toResponse).toList();

        return ShoppingProductsResponse.builder()
                .source(result.getSource())
                .products(mapped)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SearchOneResult searchOne(String keyword, int allocatedBudget) {
        String ingredientName = (keyword == null) ? "" : keyword.trim();
        int topN = 20; // Expanded to 20
        String source = "REAL";

        // 1. Normalize Query
        String normalizedQuery = queryNormalizer.normalize(ingredientName);
        if (normalizedQuery.isBlank()) {
            normalizedQuery = ingredientName; // Fallback
        }
        
        // 2. Target Category
        String targetCatNo = categoryService.findBestCategory(ingredientName).orElse(null);

        // 3. Search (TopN)
        ShoppingClientResult result = shoppingClient.searchProducts(normalizedQuery, 1, topN, targetCatNo);
        List<ShoppingProduct> products = (result == null || result.getProducts() == null)
                ? List.of()
                : result.getProducts();
        source = (result == null || result.getSource() == null) ? "NONE" : result.getSource();

        log.info("[SHOPPING_LIST][{}] SEARCH_REQ ingredient={} query=\"{}\" cat={} limit={} source={}",
                currentTraceId(), ingredientName, normalizedQuery, targetCatNo, topN, source);
        
        // Log Top 5 candidates for debugging
        logSearchTopN(ingredientName, products, 5);

        // 4. Score & Select
        ShoppingProduct best = productScorer.selectBest(products, ingredientName, allocatedBudget);
        
        ShoppingListResponse.ProductCard card = toCard(best);
        String reason = (best != null) ? "SCORER_SELECTED" : "NO_MATCH";
        
        logSelected(ingredientName, best, card, reason, allocatedBudget);

        return new SearchOneResult(card, source);
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
                .build();
    }

    private String truncate(String value, int max) {
        if (value == null) return null;
        if (value.length() <= max) return value;
        return value.substring(0, max);
    }
}
