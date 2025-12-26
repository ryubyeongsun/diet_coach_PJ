package com.dietcoach.project.dto.shopping;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetProposalResponse {
    private int budget;
    private int finalCost;
    private List<BudgetIngredientDto> ingredients;
    private String status; // "LOCKED", "WARNING_OVER_BUDGET"
    private String warningMessage; // New: User notification

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BudgetIngredientDto {
        private String name;        // Abstract name (e.g. "Chicken Breast")
        private String skuName;     // SKU name (e.g. "Frozen Chicken 2kg")
        private int price;          // SKU price
        private int quantity;
        private String link;
        private String imageUrl;
    }
}
