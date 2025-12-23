package com.dietcoach.project.dto.meal;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 하루 상세 식단 조회용 DTO
 * - 날짜, 총 kcal
 * - 아침/점심/저녁(필요시 간식까지) 메뉴 리스트
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MealPlanDayDetailResponse {

	 private Long dayId;
	    private String date;              // "2025-12-01" 형식 (String)
	    private Integer totalCalories;    // 하루 총 kcal
	    private List<MealItemResponse> items;
	    private List<MealDetailResponse> meals;

  
}
