package com.dietcoach.project.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MealItem {

    private Long id;
    private Long mealPlanDayId;

    private String mealTime;   // BREAKFAST / LUNCH / DINNER / SNACK 등 (나중에 Enum로 빼도 됨)
    private String foodName;
    private Double calories;
    private String memo;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
