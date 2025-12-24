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

    // PRD 4.1 Hard Exclusion Rules (Pet / Non-Food)
    private static final List<String> PET_BLACKLIST = List.of(
            "강아지", "애견", "반려", "개껌", "간식", "사료", 
            "캣", "고양이", "펫", "트릿", "덴탈", "스틱", "츄"
    );

    // Existing blacklist (Packaging/Bulk words that are not ingredients)
    private static final List<String> NON_INGREDIENT_TOKENS = List.of(
            "box", "set", "bundle", "gift", "random", "package",
            "박스", "상자", "세트", "선물", "랜덤", "구성", "패키지",
            "업소용", "식자재", "화분", "모종", "씨앗"
    );

    private static final List<String> NON_FOOD_TOKENS = List.of(
            "case", "storage", "container", "toy", "tool", "machine", "decor",
            "용기", "보관", "케이스", "도구", "장난감", "모형", "인테리어"
    );

    // Tokens that suggest a very large quantity
    private static final List<String> HUGE_QUANTITY_TOKENS = List.of(
            "10kg", "20kg", "100개", "50개", "대량", "말통"
    );

    public ShoppingProduct selectBest(List<ShoppingProduct> candidates, String ingredientName, int allocatedBudget) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        String traceId = MDC.get("traceId");
        String normalizedIngredient = normalize(ingredientName);

        ScoredProduct best = candidates.stream()
                .map(p -> scoreProduct(p, normalizedIngredient, allocatedBudget))
                .filter(sp -> sp.score > -2000) // Hard Drop Threshold
                .max(Comparator.comparingDouble(ScoredProduct::getFinalScore))
                .orElse(null);

        if (best != null) {
            log.info("[PRODUCT_SCORER][{}] SELECTED ingredient={} budget={} product=\"{}\" price={} score={} reason=[{}]",
                    traceId, ingredientName, allocatedBudget, best.product.getTitle(), best.product.getPrice(), 
                    String.format("%.1f", best.score), best.reason);
            return best.product;
        } else {
             log.info("[PRODUCT_SCORER][{}] NO_SELECTION ingredient={} budget={} candidates={}", 
                     traceId, ingredientName, allocatedBudget, candidates.size());
             return null;
        }
    }

    private ScoredProduct scoreProduct(ShoppingProduct p, String ingredientName, int allocatedBudget) {
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
        if (containsAny(normalizedTitle, NON_FOOD_TOKENS)) { // Category check for non-food might be too broad (e.g. 'Kitchen')
             return new ScoredProduct(p, -5000, "DROP_NONFOOD");
        }

        double score = 0;
        StringBuilder reason = new StringBuilder();

        // 2. Budget Fit Score
        double budgetFit = 0;
        if (p.getPrice() <= allocatedBudget) {
            double ratio = (double) p.getPrice() / allocatedBudget; // 0.0 ~ 1.0
            budgetFit = ratio * 50; 
            reason.append(String.format("BudgetIn(%.2f)+%.1f ", ratio, budgetFit));
        } else {
            double overRatio = (double) (p.getPrice() - allocatedBudget) / allocatedBudget;
            double penalty = overRatio * 80;
            budgetFit = -penalty;
            reason.append(String.format("BudgetOver(%.2f)-%.1f ", overRatio, penalty));
            
            if (overRatio > 2.0) {
                score -= 100;
                reason.append("HugeOver-100 ");
            }
        }
        score += budgetFit;

        // 3. Ingredient Relevance
        if (containsAny(normalizedTitle, NON_INGREDIENT_TOKENS)) {
            score -= 100;
            reason.append("NonIng-100 ");
        }

        // 4. Quantity Check
        // If > 10kg, penalty (unless ingredient implies bulk like rice?)
        // Rice usually 4kg, 10kg, 20kg.
        // Chicken 10kg is 'Business use'.
        // We use explicit tokens or parsed weight if reliable.
        if (containsAny(normalizedTitle, HUGE_QUANTITY_TOKENS)) {
            score -= 50;
            reason.append("HugeQtyToken-50 ");
        }
        
        // Check parsed weight if available
        if (p.getGramPerUnit() != null && p.getGramPerUnit() >= 10000) {
             score -= 50;
             reason.append("Over10kg-50 ");
        }

        // 5. Exact Match Bonus
        if (normalizedTitle.contains(ingredientName)) {
            score += 30;
            reason.append("ExactMatch+30 ");
        }
        
        // 6. Very Low Price Penalty (Quality Check)
        if (allocatedBudget > 2000 && p.getPrice() < allocatedBudget * 0.1) {
            score -= 30;
            reason.append("TooCheap-30 ");
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
