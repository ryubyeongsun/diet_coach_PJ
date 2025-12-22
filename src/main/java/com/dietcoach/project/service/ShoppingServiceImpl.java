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
    private final ShoppingCategoryService categoryService;

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

        // ✅ 혁신안 4: 카테고리 사전 타겟팅 (Category-Targeted Search)
        // 검색 전, 이 재료가 속할 확률이 높은 식품 카테고리 코드를 미리 확보
        String targetCatNo = categoryService.findBestCategory(ingredientName).orElse(null);

        List<String> keywordVariants = buildKeywordVariants(ingredientName);
        for (int i = 0; i < keywordVariants.size(); i++) {
            String searchKeyword = keywordVariants.get(i);
            
            // 혁신안 2: 단문 쿼리 시멘틱 보강
            if (searchKeyword.length() <= 2 && !containsAny(searchKeyword, FOOD_TOKENS)) {
                searchKeyword += " 국내산"; 
            }

            // ✅ 개선된 API 호출: 카테고리 번호(targetCatNo) 포함
            ShoppingClientResult result = shoppingClient.searchProducts(searchKeyword, 1, topN, targetCatNo);
            List<ShoppingProduct> products = (result == null || result.getProducts() == null)
                    ? List.of()
                    : result.getProducts();
            source = (result == null || result.getSource() == null) ? "NONE" : result.getSource();

            log.info("[SHOPPING_LIST][{}] SEARCH_REQ ingredient={} keyword=\"{}\" cat={} limit={} source={}",
                    currentTraceId(), ingredientName, searchKeyword, targetCatNo, topN, source);
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

            log.info("[SHOPPING_LIST][{}] NO_MATCH ingredient={} keyword=\"{}\" cat={} filtered=0",
                    currentTraceId(), ingredientName, searchKeyword, targetCatNo);
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
                    .append(" cat=").append(p.getCategoryCode())
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
        log.info("[SHOPPING_LIST][{}] SELECTED ingredient={} selected=\"{}\" code={} cat={} price={} reason={}",
                traceId,
                ingredientName,
                truncate(card.getProductName(), 40),
                product.getExternalId(),
                product.getCategoryCode(),
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
            // ✅ 카테고리 필터 적용
            boolean isValidCat = categoryService.isValidCategory(p.getCategoryCode(), p.getCategoryName());
            if (!isValidCat) {
                 log.info("[SHOPPING_LIST][{}] FILTER_REJECT ingredient={} product=\"{}\" reason=BLOCK_CATEGORY code={} name={}",
                        currentTraceId(), ingredientName, truncate(productName, 40), p.getCategoryCode(), p.getCategoryName());
                 continue;
            } else {
                 log.debug("[SHOPPING_LIST][{}] CATEGORY_PASS ingredient={} product=\"{}\" code={} name={}",
                        currentTraceId(), ingredientName, truncate(productName, 40), p.getCategoryCode(), p.getCategoryName());
            }

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

            // 점수 기준 완화: 기존 50 -> 30 (카테고리 필터가 강력하므로 점수는 조금 너그럽게)
            if (score.score() >= 30) {
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
        // ✅ 추가된 필터: 가드닝/씨앗/묘목 제외
        if (containsAny(normalizedProduct, GARDENING_TOKENS)) {
            return new HardFilterResult(false, "BLOCK_GARDENING");
        }
        // ✅ 추가된 필터: 장난감/문구/도서 제외
        if (containsAny(normalizedProduct, TOY_TOKENS)) {
            return new HardFilterResult(false, "BLOCK_TOY");
        }
        // ✅ 추가된 필터: 의류/패션 제외
        if (containsAny(normalizedProduct, FASHION_TOKENS)) {
            return new HardFilterResult(false, "BLOCK_FASHION");
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

        // ✅ 'Food' 관련 키워드가 아예 없으면 의심 (단, 예외 재료는 통과)
        boolean foodToken = containsAny(normalizedProduct, FOOD_TOKENS) || containsAny(normalizedProduct, UNIT_TOKENS);
        boolean ingredientException = containsAny(normalizedIngredient, FOOD_TOKEN_EXCEPTIONS);
        
        // 너무 엄격해서 검색결과가 0개가 되는 것을 방지하기 위해, 필터 강도를 조절하거나 점수제로 넘길 수 있음.
        // 현재는 유지하되 점수에서 승부를 봄.
        if (!foodToken && !ingredientException) {
            // return new HardFilterResult(false, "BLOCK_NOT_FOOD"); 
            // ⚠️ 너무 많이 걸러져서 '검색결과 없음'이 뜨는 경우가 많아, 하드블락 대신 점수 감점으로 전략 변경 고려
            // 여기서는 일단 주석처리 하거나, 아주 확실한 비식품만 거르는 위쪽 로직에 의존.
        }

        return new HardFilterResult(true, "OK");
    }

    private ScoreResult scoreProduct(String normalizedIngredient, String normalizedProduct) {
        int score = 0;
        StringBuilder breakdown = new StringBuilder();

        // 1. 정확도 점수
        if (!normalizedIngredient.isBlank() && normalizedProduct.contains(normalizedIngredient)) {
            score += 50;
            breakdown.append("exact+50 ");
        }

        // 2. 동의어 점수
        for (String synonym : buildSynonyms(normalizedIngredient)) {
            if (synonym.isBlank()) continue;
            if (normalizedProduct.contains(synonym)) {
                score += 20;
                breakdown.append("synonym(").append(synonym).append(")+20 ");
                break;
            }
        }

        // 3. 식재료 강력 시그널 (단위 + 푸드 키워드) -> 이게 있으면 무드등일 확률 급감
        boolean hasUnit = containsAny(normalizedProduct, UNIT_TOKENS);
        boolean hasFoodKey = containsAny(normalizedProduct, FOOD_TOKENS);

        if (hasUnit) {
            score += 30; // 단위(kg, g)가 있으면 식재료일 확률 매우 높음
            breakdown.append("hasUnit+30 ");
        }
        if (hasFoodKey) {
            score += 20; // 신선, 냉동, 식품 등
            breakdown.append("hasFoodKey+20 ");
        }

        // 4. 감점 요인
        if (containsAny(normalizedProduct, TOOL_TOKENS)) { 
            score -= 100; // 도구 키워드 있으면 강력 제외
            breakdown.append("tool-100 ");
        }
        if (containsAny(normalizedProduct, NOISE_TOKENS)) {
            score -= 10;
            breakdown.append("noise-10 ");
        }
        
        // 5. 너무 긴 제목 감점 (옵션 스팸일 확률)
        if (normalizedProduct.length() > 50) {
            score -= 5;
            breakdown.append("longTitle-5 ");
        }

        return new ScoreResult(score, breakdown.toString().trim());
    }

    private List<String> buildSynonyms(String normalizedIngredient) {
        List<String> synonyms = new ArrayList<>();
        if (containsAny(normalizedIngredient, EGG_TOKENS)) {
            synonyms.add("egg");
            synonyms.add("\uB2EC\uAC40");
            synonyms.add("\uAD6C\uC6B4\uB780"); // 구운란
            synonyms.add("\uB300\uB780"); // 대란
            synonyms.add("\uD2B9\uB780"); // 특란
        }
        if (containsAny(normalizedIngredient, HERB_TOKENS)) {
            synonyms.add("herb");
            synonyms.add("basil");
            synonyms.add("rosemary");
        }
        // 두부
        if (normalizedIngredient.contains("\uB450\uBD80")) {
            synonyms.add("\uC790\uC5F0\uCD0C");
            synonyms.add("\uD480\uBB34\uC6D0");
            synonyms.add("\uAD6D\uC0B0\uCF69");
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
            "\uACF5\uC720\uAE30", "\uC2A4\uC704\uCE6D\uD5C8\uBE0C", "\uB79C", "usb", "cable"
    );
    // 인테리어/가짜/모형
    private static final List<String> INTERIOR_TOKENS = List.of(
            "model", "fake", "interior", "decor", "prop", "light", "lamp", "led", "mood",
            "\uBAA8\uD615", "\uAC00\uC9DC", "\uC778\uD14C\uB9AC\uC5B4", "\uC18C\uD488", 
            "\uBB34\uB4DC\uB4F1", "\uC870\uBA85", "\uB7A8\uD504", "\uC804\uAD6C", "\uB370\uCF54", 
            "\uC7A5\uC2DD", "\uBCF4\uAD00\uD568", "\uBC15\uC2A4", "\uC0C1\uC790",
            "\uCEEC\uB7EC", "\uC0C9\uC0C1", "\uB514\uC790\uC778" // 컬러, 색상, 디자인
    );
    private static final List<String> ADULT_TOKENS = List.of(
            "adult", "dildo", "vibe", "condom",
            "\uC131\uC778", "\uB51C\uB3C4", "\uBC14\uC774\uBE0C", "\uCF58\uB3D4", "\uB7EC\uBE0C"
    );
    private static final List<String> BEAUTY_TOKENS = List.of(
            "toner", "essence", "cream", "mist", "serum", "soap", "shampoo",
            "\uD1A0\uB108", "\uC138\uB7FC", "\uD06C\uB9BC", "\uBE44\uB204", "\uC0F4\uD478", "\uBC14\uB514", "\uB85C\uC158"
    );
    private static final List<String> TOOL_TOKENS = List.of(
            "device", "machine", "maker", "steamer", "tool", "pan", "pot", "cooker", "warmer", "case", "holder",
            "\uCC1C\uAE30", "\uAE30\uACC4", "\uC81C\uC870\uAE30", "\uB3C4\uAD6C", "\uAE30\uAD6C", 
            "\uB0C4\uBE44", "\uD504\uB77C\uC774\uD32C", "\uD310", "\uC811\uC2DC", "\uADF8\uB987", "\uC6A9\uAE30",
            "\uCF00\uC774\uC2A4", "\uAC70\uCE58\uB300", "\uBCF4\uD638", "\uD544\uB984",
            "\uD2C0", "\uBA30\uB4DC", "\uD2B8\uB808\uC774", "\uD32C", "\uC11D\uC1e0", "\uCE7C", "\uAC00\uC704", // 틀, 몰드, 트레이, 팬, 석쇠, 칼, 가위
            "\uCEE4\uD53C\uBA38\uC2E0", "\uC8FC\uC804\uC790", "\uD3EC\uD2B8", "\uBBF9\uC11C\uAE30", // 커피머신, 주전자, 포트, 믹서기
            "\uACC4\uB780\uD2C0", "\uC694\uB9AC\uD2C0", "\uBAA8\uC591\uD2C0", "\uC5D0\uADF8\uBA30\uB4DC", "GF", // 계란틀, 요리틀, 모양틀, 에그몰드, GF
            "\uB414\uC9D1\uAC1C", "\uB414\uC9C0\uAC1C", "\uAD6D\uC790", "\uC2E4\uB9AC\uCF58", "\uC218\uC800", "\uD3EC\uD06C", "\uC813\uAC00\uB77D" // 뒤집개, 뒤지개, 국자, 실리콘, 수저, 포크, 젓가락
    );
    private static final List<String> PET_TOKENS = List.of(
            "dog", "cat", "pet", "feed", "snack",
            "\uAC15\uC544\uC9C0", "\uC560\uC644", "\uD3AB", "\uC0AC\uB8CC", "\uAC04\uC2DD", "\uB3C4\uADF8", "\uCEA3",
            "\uACE0\uC591\uC774", "\uAC1C\uAC80"
    );
    // ✅ 추가된 필터: 의류/패션 제외
    private static final List<String> FASHION_TOKENS = List.of(
            "knit", "pants", "dress", "shirt", "jacket", "coat", "socks",
            "\uB2C8\uD2B8", "\uBC14\uC9C0", "\uD32C\uCE20", "\uC6D0\uD53C\uC2A4", "\uC154\uCE20", "\uC790\uBCC3", 
            "\uCE58\uB9C8", "\uCF54\uD2B8", "\uBAA8\uC790", "\uC591\uB9AC", "\uD2F0\uC154\uCE20", "\uBE0C\uB7AC\uC9C0\uC5B4"
    );
    // 씨앗/가드닝
    private static final List<String> GARDENING_TOKENS = List.of(
            "seed", "plant", "grow", "pot",
            "\uC528\uC557", "\uC885\uC790", "\uBBAC\uBAA9", "\uBAA8\uC885", "\uD654\uBD04", "\uD0A4\uC6B0\uAE30", "\uD0A4\uD2B8", "\uC7AC\uBC30"
    );
    // 장난감/문구
    private static final List<String> TOY_TOKENS = List.of(
            "toy", "doll", "game", "book",
            "\uC7A5\uB09C\uAC10", "\uC778\uD615", "\uAC8C\uC784", "\uB3C4\uC11C", "\uCC45", "\uC2A4\uD2F0\uCEE4", "\uBB38\uAD6C"
    );

    private static final List<String> SNACK_TOKENS = List.of(
            "chip", "snack", "cracker", "biscuit", "popcorn",
            "\uCE69", "\uACFC\uC790", "\uC2A4\uB0C9", "\uBF55\uD280\uAE30", "\uD06C\uB798\uCEE4",
            "\uBE44\uC2A4\uD0B7", "\uAC15\uC815", "\uD31D\uCF58", "\uD280\uBC25", "\uAC74\uBE75",
            "\uC5D0\uADF8\uB864" // 에그롤 추가
    );
    private static final List<String> TOMATO_TOKENS = List.of("tomato", "\uD1A0\uB9C8\uD1A0");
    private static final List<String> TOMATO_SAUCE_TOKENS = List.of(
            "ketchup", "sauce", "puree", "paste", "dressing",
            "\uCF00\uCC39", "\uC18C\uC2A4", "\uD4E8\uB808", "\uD398\uC774\uC2A4\uD2B8", "\uB4DC\uB808\uC2F1"
    );
    private static final List<String> FOOD_TOKENS = List.of(
            "fresh", "frozen", "food", "farm", "eat",
            "\uAD6D\uB0B4\uC0B0", "\uC218\uC785", "\uB0C9\uB3D9", "\uC0DD", "\uC2DD\uD488",
            "\uB18D\uC7A5", "\uC0B0\uC9C0", "\uBB34\uB18D\uC57D", "\uC720\uAE30\uB18D", "\uB2F9\uC77C", 
            "\uBC1C\uC1A1", "\uB9DB\uC788\uB294", "\uD587"
    );
    private static final List<String> UNIT_TOKENS = List.of(
            "kg", "g", "ml", "l", "pack", "ea",
            "\uAD6C", "\uBD09", "\uD329", "\uB9AC\uD130" // liter
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
