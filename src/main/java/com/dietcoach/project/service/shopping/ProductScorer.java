package com.dietcoach.project.service.shopping;

import com.dietcoach.project.domain.ShoppingProduct;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Slf4j
@Component
public class ProductScorer {

    // PRD 4-2 Strict Blacklist Rules
    private static final List<String> PET_BLACKLIST = List.of(
            "강아지", "애견", "반려", "개껌", "간식", "사료", 
            "캣", "고양이", "펫", "트릿", "덴탈", "스틱", "츄"
    );

    // PRD 4-2 Explicit Exclude Keywords
    private static final List<String> NON_INGREDIENT_TOKENS = List.of(
            "박스", "세트", "묶음", "대용량", "사은품", // PRD strict excludes
            "box", "set", "bundle", "package", "gift", "random",
            "선물", "선물세트", "랜덤", "업소용", "식자재", "화분", "모종", "씨앗",
            "짝", "궤", "콘테이너"
    );

    private static final List<String> NON_FOOD_TOKENS = List.of(
            "장난감", "접시", "그릇", // PRD strict excludes
            "case", "storage", "container", "toy", "tool", "machine", "decor",
            "용기", "보관", "케이스", "도구", "모형", "인테리어",
            "냄비", "뚝배기", "컵", "팬", "수세미", "주방", "용품", "식기",
            "메이커", "머신", "몰드", "틀", "채반", "거름망", "장갑", "행주", "세제", "비누",
            "찜기", "슬라이서", "다지기", "칼", "도마", "국자", "뒤집개"
    );

    // Tokens that suggest a very large quantity
    private static final List<String> HUGE_QUANTITY_TOKENS = List.of(
            "10kg", "20kg", "100개", "50개", "말통", "박스"
    );

    private static final List<String> PROCESSED_TOKENS = List.of(
            "주스", "즙", "소스", "케첩", "가루", "분말", "퓨레", "농축", "캔", "통조림", "조미"
    );

    public List<ShoppingProduct> selectTopN(List<ShoppingProduct> candidates, String ingredientName, int allocatedBudget, Long totalGram, int n) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        String normalizedIngredient = normalize(ingredientName);

        return candidates.stream()
                .map(p -> scoreProduct(p, normalizedIngredient, allocatedBudget, totalGram))
                .filter(sp -> sp.score > -2000) // Hard Drop Threshold
                .sorted(Comparator.comparingDouble(ScoredProduct::getFinalScore).reversed()) // Descending score
                .limit(n)
                .map(sp -> sp.product)
                .toList();
    }

    public ShoppingProduct selectBest(List<ShoppingProduct> candidates, String ingredientName, int allocatedBudget, Long totalGram) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        String traceId = MDC.get("traceId");
        String normalizedIngredient = normalize(ingredientName);

        ScoredProduct best = candidates.stream()
                .map(p -> scoreProduct(p, normalizedIngredient, allocatedBudget, totalGram))
                .filter(sp -> sp.score > -2000) // Hard Drop Threshold
                .max(Comparator.comparingDouble(ScoredProduct::getFinalScore))
                .orElse(null);

        if (best != null) {
            log.info("[PRODUCT_SCORER][{}] SELECTED ingredient={} budget={} gram={} product=\"{}\" price={} score={} reason=[{}]",
                    traceId, ingredientName, allocatedBudget, totalGram, best.product.getTitle(), best.product.getPrice(), 
                    String.format("%.1f", best.score), best.reason);
            return best.product;
        } else {
             log.info("[PRODUCT_SCORER][{}] NO_SELECTION ingredient={} budget={} gram={} candidates={}", 
                     traceId, ingredientName, allocatedBudget, totalGram, candidates.size());
             return null;
        }
    }

    private ScoredProduct scoreProduct(ShoppingProduct p, String ingredientName, int allocatedBudget, Long totalGram) {
        if (p == null || p.getTitle() == null || p.getPrice() <= 0) {
            return new ScoredProduct(p, -9999, "INVALID");
        }

        String title = p.getTitle();
        String category = (p.getCategoryName() != null) ? p.getCategoryName() : "";
        String normalizedTitle = normalize(title);
        String normalizedCategory = normalize(category);
        
        // 1. HARD DROP: Pet Food / Blacklist (Check Title AND Category)
        if (containsAny(normalizedTitle, PET_BLACKLIST) || containsAny(normalizedCategory, PET_BLACKLIST)) {
            return new ScoredProduct(p, -5000, "DROP_PET");
        }
        if (containsAny(normalizedTitle, NON_FOOD_TOKENS)) { 
             return new ScoredProduct(p, -5000, "DROP_NONFOOD");
        }
        // PRD 4-2 Strict Filter: Box/Set/Bulk
        if (containsAny(normalizedTitle, NON_INGREDIENT_TOKENS)) {
             return new ScoredProduct(p, -4000, "DROP_BLACKLIST");
        }
        if (!containsAny(ingredientName, PROCESSED_TOKENS) && containsAny(normalizedTitle, PROCESSED_TOKENS)) {
            return new ScoredProduct(p, -3500, "DROP_PROCESSED");
        }

        double score = 0;
        StringBuilder reason = new StringBuilder();

        // 2. Budget Fit Score (Enhanced with TotalGram)
        long projectedTotalCost = p.getPrice();
        double quantityRatio = 1.0;
        
        if (totalGram != null && totalGram > 0 && p.getGramPerUnit() != null && p.getGramPerUnit() > 0) {
            // Count needed to fulfill totalGram
            int count = (int) Math.ceil((double) totalGram / p.getGramPerUnit());
            projectedTotalCost = (long) p.getPrice() * count;
            
            // Quantity Ratio: Product Size / Required Size
            quantityRatio = (double) p.getGramPerUnit() / totalGram;
            reason.append(String.format("cnt=%d ", count));
        }

        if (allocatedBudget > 0) {
            if (projectedTotalCost > allocatedBudget * 2.0) {
                return new ScoredProduct(p, -3000, "DROP_OVER_BUDGET");
            }
            if (projectedTotalCost <= allocatedBudget * 1.3) { // Allow 30% overage
                double ratio = (double) projectedTotalCost / allocatedBudget; // 0.0 ~ 1.3
                // Perfect fit is 1.0. Lower is cheaper.
                if (ratio <= 1.0) {
                    double budgetFit = ratio * 50; 
                    score += budgetFit;
                    reason.append(String.format("BudgetIn(%.2f)+%.1f ", ratio, budgetFit));
                } else {
                    score += 5;
                    reason.append("BudgetSlightOver+5 ");
                }
            } else {
                // Over Budget > 30%
                double overRatio = (double) (projectedTotalCost - allocatedBudget) / allocatedBudget;
                double penalty = overRatio * 120; 
                if (penalty > 500) penalty = 500;
                score -= penalty;
                reason.append(String.format("BudgetOver(%.2f)-%.1f ", overRatio, penalty));
            }
        }
        
        // 3. Quantity Fit Score (Avoid too small or too huge products)
        if (totalGram != null && totalGram > 0 && p.getGramPerUnit() != null) {
            if (quantityRatio > 5.0) { // Product is 5x bigger than needed
                score -= 50;
                reason.append("TooBig-50 ");
            } else if (quantityRatio < 0.2) { // Product is 1/5th of needed (need 5 packs)
                score -= 30;
                reason.append("TooSmall-30 ");
            } else {
                score += 20; // Good size match
                reason.append("GoodSize+20 ");
            }
            if (p.getGramPerUnit() > totalGram * 3) {
                score -= 80;
                reason.append("OverNeed-80 ");
            }
        }

        // 4. Quantity Check (Huge)
        if (containsAny(normalizedTitle, HUGE_QUANTITY_TOKENS)) {
            score -= 50;
            reason.append("HugeQtyToken-50 ");
        }
        if (p.getGramPerUnit() != null && p.getGramPerUnit() >= 20000) { // Limit to 20kg
             score -= 50;
             reason.append("Over20kg-50 ");
        }

        // 5. Exact Match Bonus
        if (normalizedTitle.contains(ingredientName)) {
            score += 30;
            reason.append("ExactMatch+30 ");
        }
        
        // 6. Very Low Price Penalty (Quality Check) - Check against single unit price
        // If single unit price is suspiciously low (e.g. 100 won), it's likely trash data or accessory
        if (allocatedBudget > 2000 && p.getPrice() < 500) { 
            score -= 100;
            reason.append("TooCheapUnit-100 ");
        }

        return new ScoredProduct(p, score, reason.toString().trim());
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private boolean containsAny(String text, List<String> tokens) {
        for (String token : tokens) {
            if (text.contains(token)) return true;
        }
        return false;
    }

    private static class ScoredProduct {
        ShoppingProduct product;
        double score;
        String reason;

        ScoredProduct(ShoppingProduct product, double score, String reason) {
            this.product = product;
            this.score = score;
            this.reason = reason;
        }
        
        double getFinalScore() {
            return score;
        }
    }
}
