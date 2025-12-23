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

    // Blacklist / Penalty Tokens
    private static final List<String> BLACKLIST_TOKENS = List.of(
            "box", "set", "bundle", "gift", "random", "package",
            "박스", "상자", "세트", "선물", "대용량", "묶음", "랜덤", "구성", "패키지",
            "업소용", "식자재"
    );

    private static final List<String> NON_FOOD_TOKENS = List.of(
            "case", "storage", "container", "toy", "tool", "machine", "decor",
            "용기", "보관", "케이스", "도구", "장난감", "모형", "인테리어", "씨앗", "묘목", "재배", "화분"
    );

    // Tokens that suggest a very large quantity
    private static final List<String> HUGE_QUANTITY_TOKENS = List.of(
            "10kg", "20kg", "100개", "50개", "대량"
    );

    public ShoppingProduct selectBest(List<ShoppingProduct> candidates, String ingredientName, int allocatedBudget) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        String traceId = MDC.get("traceId");
        String normalizedIngredient = normalize(ingredientName);

        ScoredProduct best = candidates.stream()
                .map(p -> scoreProduct(p, normalizedIngredient, allocatedBudget))
                .filter(sp -> sp.score > -1000) // Filter out hard fails
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
        String normalizedTitle = normalize(title);
        double score = 0;
        StringBuilder reason = new StringBuilder();

        // 1. Budget Fit Score
        double budgetFit = 0;
        if (p.getPrice() <= allocatedBudget) {
            // Price is within budget. Higher price (closer to budget) is slightly better to avoid very cheap/low quality items,
            // but we also want to save money. 
            // Strategy: Close to budget is good (utilizing budget), but not strictly.
            // Let's maximize 'closeness' to budget from below? 
            // Or just "Pass". 
            // PRD: "If <= budget: + (1 - (budget - price)/budget) * 50" -> Higher price (closer to budget) gets more points.
            // This favors higher quality items within budget.
            double ratio = (double) p.getPrice() / allocatedBudget; // 0.0 ~ 1.0
            budgetFit = ratio * 50; 
            reason.append(String.format("BudgetIn(%.2f)+%.1f ", ratio, budgetFit));
        } else {
            // Price exceeds budget. Penalize.
            // PRD: "- (price - budget)/budget * 80"
            double overRatio = (double) (p.getPrice() - allocatedBudget) / allocatedBudget;
            double penalty = overRatio * 80;
            budgetFit = -penalty;
            reason.append(String.format("BudgetOver(%.2f)-%.1f ", overRatio, penalty));
            
            // Extreme over budget (e.g. > 3x) -> Huge penalty
            if (overRatio > 2.0) {
                score -= 100;
                reason.append("HugeOver-100 ");
            }
        }
        score += budgetFit;

        // 2. Keyword Penalty (Blacklist)
        if (containsAny(normalizedTitle, BLACKLIST_TOKENS)) {
            score -= 200;
            reason.append("Blacklist-200 ");
        }
        if (containsAny(normalizedTitle, NON_FOOD_TOKENS)) {
            score -= 300; // Stronger penalty for non-food
            reason.append("NonFood-300 ");
        }
        if (containsAny(normalizedTitle, HUGE_QUANTITY_TOKENS)) {
            score -= 50;
            reason.append("HugeQty-50 ");
        }

        // 3. Exact Match Bonus
        if (normalizedTitle.contains(ingredientName)) {
            score += 30;
            reason.append("ExactMatch+30 ");
        }
        
        // 4. Very Low Price Penalty (Quality Check)
        // If price is < 10% of allocated budget (and allocated budget is reasonable, e.g. > 2000), it might be trash or accessory
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
