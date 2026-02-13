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

	 private String ingredientName;  // food_name 기준
	    private Integer totalGram;      // 아직 gram 정보 없으면 null
	    private Integer totalCalories;  // SUM(calories)
	    private Integer daysCount;      // 몇 일의 식단에 등장했는지 (COUNT(DISTINCT day))
}
