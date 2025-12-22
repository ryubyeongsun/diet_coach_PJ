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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShoppingServiceImpl implements ShoppingService {

    private final ShoppingClient shoppingClient;

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
    public SearchOneResult searchOne(String keyword) {
        String ingredientName = (keyword == null) ? "" : keyword.trim();
        int topN = 20;
        String source = "REAL";

        List<String> keywordVariants = buildKeywordVariants(ingredientName);
        for (int i = 0; i < keywordVariants.size(); i++) {
            String searchKeyword = keywordVariants.get(i);
            ShoppingClientResult result = shoppingClient.searchProducts(searchKeyword, 1, topN);
            List<ShoppingProduct> products = (result == null || result.getProducts() == null)
                    ? List.of()
                    : result.getProducts();
            source = (result == null || result.getSource() == null) ? "NONE" : result.getSource();

            log.info("[SHOPPING_LIST][{}] SEARCH_REQ ingredient={} keyword=\"{}\" limit={} source={}",
                    currentTraceId(), ingredientName, searchKeyword, topN, source);
            logSearchTopN(ingredientName, products, 5);

            List<ScoredProduct> scored = filterAndScore(ingredientName, products);
            if (!scored.isEmpty()) {
                scored.sort(Comparator.comparingInt(ScoredProduct::score).reversed()
                        .thenComparingLong(ScoredProduct::priceSafe));
                ScoredProduct best = scored.get(0);
                ShoppingListResponse.ProductCard card = toCard(best.product());
                String reason = (i == 0) ? "SCORE_TOP1" : "FALLBACK_KEYWORD_RETRY";
                logSelected(ingredientName, best.product(), card, reason);
                return new SearchOneResult(card, source);
            }

            log.info("[SHOPPING_LIST][{}] NO_MATCH ingredient={} keyword=\"{}\" filtered=0",
                    currentTraceId(), ingredientName, searchKeyword);
        }

        logSelected(ingredientName, null, null, "EMPTY_ALL");
        return new SearchOneResult(null, "NONE");
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
                    .append(" name=\"").append(truncate(p.getTitle(), 40)).append("\"")
                    .append(" price=").append(p.getPrice())
                    .append(" mall=").append(p.getMallName())
                    .append(" code=").append(p.getExternalId());
        }

        log.info("[SHOPPING_LIST][{}] SEARCH_TOP{} ingredient={} {}",
                traceId, topN, ingredientName, summary);
    }

    private void logSelected(String ingredientName, ShoppingProduct product, ShoppingListResponse.ProductCard card,
            String reason) {
        String traceId = currentTraceId();
        if (product == null || card == null) {
            log.info("[SHOPPING_LIST][{}] SELECTED ingredient={} selected=null reason={}",
                    traceId, ingredientName, reason);
            return;
        }
        log.info("[SHOPPING_LIST][{}] SELECTED ingredient={} selected=\"{}\" code={} price={} reason={}",
                traceId,
                ingredientName,
                truncate(card.getProductName(), 40),
                product.getExternalId(),
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

    private List<String> buildKeywordVariants(String ingredientName) {
        List<String> variants = new ArrayList<>();
        String base = (ingredientName == null) ? "" : ingredientName.trim();
        if (!base.isBlank()) {
            variants.add(base);
            variants.addAll(buildRetryKeywords(base));
        } else {
            variants.add("food");
        }
        return variants;
    }

    private List<String> buildRetryKeywords(String base) {
        List<String> retries = new ArrayList<>();
        String normalized = normalizeKey(base);
        if (containsAny(normalized, FRESH_INGREDIENT_TOKENS)) {
            retries.add(base + " \uC0DD\uACFC\uC77C");
            retries.add(base + " \uC0DD\uCC44\uC18C");
        } else {
            retries.add(base + " \uAD6D\uB0B4\uC0B0");
            retries.add(base + " 1kg");
        }
        return retries;
    }

    private List<ScoredProduct> filterAndScore(String ingredientName, List<ShoppingProduct> products) {
        List<ScoredProduct> result = new ArrayList<>();
        if (products == null || products.isEmpty()) {
            return result;
        }

        String normalizedIngredient = normalizeKey(ingredientName);
        for (ShoppingProduct p : products) {
            String productName = (p == null) ? "" : p.getTitle();
            String normalizedProduct = normalizeKey(productName);

            HardFilterResult filter = applyHardFilter(normalizedIngredient, normalizedProduct);
            if (!filter.allowed()) {
                log.info("[SHOPPING_LIST][{}] FILTER_REJECT ingredient={} product=\"{}\" reason={}",
                        currentTraceId(),
                        ingredientName,
                        truncate(productName, 40),
                        filter.reason());
                continue;
            }

            ScoreResult score = scoreProduct(normalizedIngredient, normalizedProduct);
            log.info("[SHOPPING_LIST][{}] SCORE ingredient={} product=\"{}\" score={} breakdown={}",
                    currentTraceId(),
                    ingredientName,
                    truncate(productName, 40),
                    score.score(),
                    score.breakdown());

            if (score.score() >= 50) {
                result.add(new ScoredProduct(p, score.score()));
            }
        }
        return result;
    }

    private HardFilterResult applyHardFilter(String normalizedIngredient, String normalizedProduct) {
        if (normalizedProduct.isBlank()) {
            return new HardFilterResult(false, "EMPTY_PRODUCT_NAME");
        }

        if (containsAny(normalizedProduct, PET_TOKENS)) {
            return new HardFilterResult(false, "BLOCK_PET");
        }
        if (containsAny(normalizedProduct, NETWORK_TOKENS)) {
            return new HardFilterResult(false, "BLOCK_NETWORK");
        }
        if (containsAny(normalizedProduct, INTERIOR_TOKENS)) {
            return new HardFilterResult(false, "BLOCK_INTERIOR");
        }
        if (containsAny(normalizedProduct, ADULT_TOKENS)) {
            return new HardFilterResult(false, "BLOCK_ADULT");
        }
        if (containsAny(normalizedProduct, BEAUTY_TOKENS)) {
            return new HardFilterResult(false, "BLOCK_BEAUTY");
        }
        if (containsAny(normalizedProduct, TOOL_TOKENS)) {
            return new HardFilterResult(false, "BLOCK_TOOL");
        }

        if (containsAny(normalizedIngredient, FRESH_INGREDIENT_TOKENS) && containsAny(normalizedProduct, SNACK_TOKENS)) {
            return new HardFilterResult(false, "BLOCK_SNACK_FOR_FRESH");
        }
        if (containsAny(normalizedIngredient, TOMATO_TOKENS) && containsAny(normalizedProduct, TOMATO_SAUCE_TOKENS)) {
            return new HardFilterResult(false, "BLOCK_TOMATO_SAUCE");
        }

        if (containsAny(normalizedIngredient, HERB_TOKENS) && containsAny(normalizedProduct, NETWORK_TOKENS)) {
            return new HardFilterResult(false, "BLOCK_HERB_NETWORK");
        }
        if (containsAny(normalizedIngredient, EGG_TOKENS) && containsAny(normalizedProduct, TOOL_TOKENS)) {
            return new HardFilterResult(false, "BLOCK_EGG_TOOL");
        }

        boolean foodToken = containsAny(normalizedProduct, FOOD_TOKENS) || containsAny(normalizedProduct, UNIT_TOKENS);
        boolean ingredientException = containsAny(normalizedIngredient, FOOD_TOKEN_EXCEPTIONS);
        if (!foodToken && !ingredientException) {
            return new HardFilterResult(false, "BLOCK_NOT_FOOD");
        }

        return new HardFilterResult(true, "OK");
    }

    private ScoreResult scoreProduct(String normalizedIngredient, String normalizedProduct) {
        int score = 0;
        StringBuilder breakdown = new StringBuilder();

        if (!normalizedIngredient.isBlank() && normalizedProduct.contains(normalizedIngredient)) {
            score += 60;
            breakdown.append("ingredientExact+60 ");
        }

        for (String synonym : buildSynonyms(normalizedIngredient)) {
            if (synonym.isBlank()) {
                continue;
            }
            if (normalizedProduct.contains(synonym)) {
                score += 20;
                breakdown.append("synonym(").append(synonym).append(")+20 ");
                break;
            }
        }

        if (containsAny(normalizedProduct, UNIT_TOKENS)) {
            score += 10;
            breakdown.append("unit+10 ");
        }
        if (containsAny(normalizedProduct, FOOD_TOKENS)) {
            score += 10;
            breakdown.append("foodToken+10 ");
        }
        if (containsAny(normalizedProduct, TOOL_TOKENS)) {
            score -= 50;
            breakdown.append("tool-50 ");
        }
        if (containsAny(normalizedProduct, NOISE_TOKENS)) {
            score -= 10;
            breakdown.append("noise-10 ");
        }

        return new ScoreResult(score, breakdown.toString().trim());
    }

    private List<String> buildSynonyms(String normalizedIngredient) {
        List<String> synonyms = new ArrayList<>();
        if (containsAny(normalizedIngredient, EGG_TOKENS)) {
            synonyms.add("egg");
            synonyms.add("\uB2EC\uAC40");
        }
        if (containsAny(normalizedIngredient, HERB_TOKENS)) {
            synonyms.add("herb");
            synonyms.add("basil");
            synonyms.add("rosemary");
        }
        return synonyms;
    }

    private String normalizeKey(String value) {
        if (value == null) {
            return "";
        }
        String cleaned = value.trim().toLowerCase(Locale.ROOT);
        cleaned = cleaned.replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
        return cleaned.replaceAll("\\s+", " ");
    }

    private boolean containsAny(String haystack, List<String> tokens) {
        if (haystack == null || haystack.isBlank() || tokens == null || tokens.isEmpty()) {
            return false;
        }
        for (String token : tokens) {
            if (token == null || token.isBlank()) {
                continue;
            }
            if (haystack.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private static final List<String> NETWORK_TOKENS = List.of(
            "iptime", "router", "switch", "hub", "lan", "port", "gigabit",
            "\uACF5\uC720\uAE30", "\uC2A4\uC704\uCE6D\uD5C8\uBE0C", "\uB79C"
    );
    private static final List<String> INTERIOR_TOKENS = List.of(
            "model", "fake", "interior", "decor", "prop",
            "\uBAA8\uD615", "\uAC00\uC9DC", "\uC778\uD14C\uB9AC\uC5B4", "\uC18C\uD488"
    );
    private static final List<String> ADULT_TOKENS = List.of(
            "adult", "dildo", "vibe", "condom",
            "\uC131\uC778", "\uB51C\uB3C4", "\uBC14\uC774\uBE0C", "\uCF58\uB3D4", "\uB7EC\uBE0C"
    );
    private static final List<String> BEAUTY_TOKENS = List.of(
            "toner", "essence", "cream", "mist", "serum",
            "\uD1A0\uB108", "\uC138\uB7FC", "\uD06C\uB9BC"
    );
    private static final List<String> TOOL_TOKENS = List.of(
            "device", "machine", "maker", "steamer", "tool",
            "\uCC1C\uAE30", "\uAE30\uACC4", "\uC81C\uC870\uAE30", "\uB3C4\uAD6C", "\uAE30\uAD6C"
    );
    private static final List<String> PET_TOKENS = List.of(
            "dog", "cat", "pet", "feed", "snack",
            "\uAC15\uC544\uC9C0", "\uC560\uC644", "\uD3AB", "\uC0AC\uB8CC", "\uAC04\uC2DD", "\uB3C4\uADF8", "\uCEA3"
    );
    private static final List<String> SNACK_TOKENS = List.of(
            "chip", "snack", "cracker", "biscuit", "popcorn",
            "\uCE69", "\uACFC\uC790", "\uC2A4\uB0C9", "\uBF55\uD280\uAE30", "\uD06C\uB798\uCEE4",
            "\uBE44\uC2A4\uD0B7", "\uAC15\uC815", "\uD31D\uCF58", "\uD280\uBC25", "\uAC74\uBE75"
    );
    private static final List<String> TOMATO_TOKENS = List.of("tomato", "\uD1A0\uB9C8\uD1A0");
    private static final List<String> TOMATO_SAUCE_TOKENS = List.of(
            "ketchup", "sauce", "puree", "paste", "dressing",
            "\uCF00\uCC39", "\uC18C\uC2A4", "\uD4E8\uB808", "\uD398\uC774\uC2A4\uD2B8", "\uB4DC\uB808\uC2F1"
    );
    private static final List<String> FOOD_TOKENS = List.of(
            "fresh", "frozen", "food", "farm", "eat", "g", "kg", "ml", "l",
            "\uAD6D\uB0B4\uC0B0", "\uC218\uC785", "\uB0C9\uB3D9", "\uC0DD", "\uC2DD\uD488",
            "\uB18D\uC7A5", "\uC0B0\uC9C0"
    );
    private static final List<String> UNIT_TOKENS = List.of(
            "kg", "g", "ml", "l", "pack", "box", "ea",
            "\uAD6C", "\uBD09", "\uD329"
    );
    private static final List<String> NOISE_TOKENS = List.of("set", "bundle", "\uC138\uD2B8");
    private static final List<String> HERB_TOKENS = List.of("herb", "\uD5C8\uBE0C");
    private static final List<String> EGG_TOKENS = List.of("egg", "\uACC4\uB780", "\uB2EC\uAC40");
    private static final List<String> FRESH_INGREDIENT_TOKENS = List.of(
            "banana", "tomato", "carrot", "spinach", "lettuce", "salad", "cabbage", "broccoli", "cucumber",
            "apple", "grape", "orange", "berry", "strawberry", "pepper",
            "\uBC14\uB098\uB098", "\uD1A0\uB9C8\uD1A0", "\uB2F9\uADFC", "\uC2DC\uAE08\uCE58", "\uC0C1\uCD94",
            "\uC591\uC0C1\uCD94", "\uC0D0\uB7EC\uB4DC", "\uC591\uBC30\uCD94", "\uBE0C\uB85C\uCF5C\uB9AC",
            "\uC624\uC774", "\uC0AC\uACFC", "\uD3EC\uB3C4", "\uC624\uB80C\uC9C0", "\uB538\uAE30",
            "\uD53C\uB9DD", "\uD30C\uD504\uB9AC\uCE74"
    );
    private static final List<String> FOOD_TOKEN_EXCEPTIONS = List.of(
            "salt", "sugar", "rice", "\uC18C\uAE08", "\uC124\uD0D5", "\uBC25", "\uC300"
    );

    private record HardFilterResult(boolean allowed, String reason) {}
    private record ScoreResult(int score, String breakdown) {}
    private record ScoredProduct(ShoppingProduct product, int score) {
        long priceSafe() {
            return (product == null) ? Long.MAX_VALUE : product.getPrice();
        }
    }

    private String truncate(String value, int max) {
        if (value == null) return null;
        if (value.length() <= max) return value;
        return value.substring(0, max);
    }
}
