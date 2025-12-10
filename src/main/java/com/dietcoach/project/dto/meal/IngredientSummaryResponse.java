package com.dietcoach.project.dto.meal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 식단 전체에서 사용되는 재료 요약 DTO
 * - name: 재료 이름 ("닭가슴살")
 * - totalGram: 총 필요 g 수
 * - mealCount: 이 재료가 들어간 식사 횟수 (옵션)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IngredientSummaryResponse {

    private String name;
    private int totalGram;
    private int mealCount;
}
