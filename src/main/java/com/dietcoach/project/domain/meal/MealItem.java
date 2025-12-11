package com.dietcoach.project.domain.meal;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealItem {

    private Long id;
    private Long mealPlanDayId;

    private String mealTime;  // "BREAKFAST", "LUNCH", "DINNER", "SNACK"
    private String foodName;
    private Integer calories;
    private String memo;
    private Integer grams;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
