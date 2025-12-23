package com.dietcoach.project.dto.meal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 하루 식단 내 한 끼(아침/점심/저녁/간식) 정보 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MealItemResponse {

    private Long id;           // meal_items.id
    private String mealTime;   // BREAKFAST / LUNCH / DINNER / SNACK 등
    private String foodName;
    private Integer calories;
    private Integer carbs;
    private Integer protein;
    private Integer fat;
    private Boolean isHighProtein;
    private Integer grams; 
    private String memo;
    
}
