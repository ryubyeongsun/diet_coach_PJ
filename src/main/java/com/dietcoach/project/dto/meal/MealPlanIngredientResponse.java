package com.dietcoach.project.dto.meal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 쇼핑 연동용 재료 요약 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MealPlanIngredientResponse {

    private String ingredient; // "닭가슴살"
    private int neededGram;    // 3200 (g)
}
