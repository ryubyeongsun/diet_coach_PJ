package com.dietcoach.project.client.ai.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiMonthlySkeletonResponse {
    private List<AiDaySkeleton> days;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AiDaySkeleton {
        private Integer dayIndex;
        private String planDate; // "yyyy-MM-dd"
        private AiValidation validation;
        private List<AiMealSkeleton> meals;
        
        // Deprecated: kept for compatibility if needed, but validation.actualTotalKcal is preferred
        private Integer totalCalories; 
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AiValidation {
        private Integer targetKcal;
        private Integer actualTotalKcal;
        private Integer estimatedCostKrw;
        private Boolean isBudgetSafe;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AiMealSkeleton {
        private String mealTime;      // BREAKFAST|LUNCH|DINNER|SNACK
        private String menuName;      // "Oatmeal bowl"
        private String reasoning;     // "Why this meal?"
        private List<AiIngredient> ingredients; 
        private Integer calories;     // Meal total calories

        @JsonSetter("ingredients")
        public void setIngredients(List<?> rawIngredients) {
            if (rawIngredients == null) {
                this.ingredients = null;
                return;
            }
            List<AiIngredient> mapped = new ArrayList<>();
            for (Object raw : rawIngredients) {
                if (raw == null) continue;
                if (raw instanceof String s) {
                    mapped.add(AiIngredient.builder().ingredientName(s).build());
                    continue;
                }
                if (raw instanceof Map<?, ?> map) {
                    Object name = map.get("ingredientName");
                    if (name == null) name = map.get("name");
                    
                    Object grams = map.get("grams");
                    
                    Object calories = map.get("calories");
                    if (calories == null) calories = map.get("kcal");

                    Object costTier = map.get("costTier");

                    mapped.add(AiIngredient.builder()
                            .ingredientName(name == null ? null : name.toString())
                            .grams(toIntegerSafe(grams))
                            .calories(toIntegerSafe(calories))
                            .costTier(costTier == null ? null : costTier.toString())
                            .build());
                    continue;
                }
                mapped.add(AiIngredient.builder().ingredientName(raw.toString()).build());
            }
            this.ingredients = mapped;
        }

        private Integer toIntegerSafe(Object v) {
            if (v == null) return null;
            if (v instanceof Integer i) return i;
            if (v instanceof Number n) return n.intValue();
            try {
                return Integer.parseInt(v.toString());
            } catch (Exception e) {
                return null;
            }
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AiIngredient {
        @JsonAlias("name")
        private String ingredientName;
        
        private Integer grams;
        
        @JsonAlias("kcal")
        private Integer calories;
        
        private String costTier; // "Low", "Medium", "High"
    }
}
