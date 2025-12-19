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
    private LocalDate fromDate;
    private LocalDate toDate;
    private List<ShoppingItem> items;

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    @Builder
    public static class ShoppingItem {
        private String ingredientName;
        private Long totalGram;
        private Integer daysCount;
        private ProductCard product;
        private String source; // REAL|MOCK
    }

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    @Builder
    public static class ProductCard {
        private String name;
        private Long price;
        private String imageUrl;
        private String detailUrl;
    }
}
