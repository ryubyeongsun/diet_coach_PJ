package com.dietcoach.project.dto.meal;

import java.time.LocalDate;
import java.util.List;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ShoppingListResponse {
    private Long planId;
    private String range;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long budget; // ✅ Allocated Budget for this range
    private String source; // REAL|MOCK
    private List<ShoppingItem> items;

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    @Builder
    public static class ShoppingItem {
        private String ingredientName;
        private Long totalGram;
        private Integer totalCalories;
        private Integer daysCount;
        private ProductCard product;
        private String source; // REAL|MOCK
    }

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    @Builder
    public static class ProductCard {
        private String productName;
        private Long price;
        private String imageUrl;
        private String productUrl;
    }
}
