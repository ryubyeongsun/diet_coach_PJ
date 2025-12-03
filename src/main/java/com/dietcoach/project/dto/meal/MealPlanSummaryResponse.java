package com.dietcoach.project.dto.meal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MealPlanSummaryResponse {

    private Long mealPlanId;
    private Long userId;

    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalDays;
    private Double targetCaloriesPerDay;
}
