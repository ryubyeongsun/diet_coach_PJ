package com.dietcoach.project.dto.meal;

import java.time.LocalDate;
import java.util.List;

import com.dietcoach.project.domain.meal.MealPlan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 한 달 식단 전체 요약 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MealPlanOverviewResponse {

    private Long mealPlanId;
    private Long userId;

    private LocalDate startDate;
    private LocalDate endDate;

    private int targetCaloriesPerDay;

    private List<MealPlanDaySummaryResponse> days;
    private Long monthlyBudget;
    private Integer mealsPerDay;
    private List<String> preferences;
    private List<String> allergies;

    public static MealPlanOverviewResponse of(
            MealPlan plan,
            List<MealPlanDaySummaryResponse> daySummaries
    ) {
        return MealPlanOverviewResponse.builder()
                .mealPlanId(plan.getId())
                .userId(plan.getUserId())
                .startDate(plan.getStartDate())
                .endDate(plan.getEndDate())
                .targetCaloriesPerDay(plan.getTargetCaloriesPerDay()) // Integer → int 오토언박싱
                .days(daySummaries)
                .build();
    }
}
