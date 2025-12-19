package com.dietcoach.project.dto.meal;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.dietcoach.project.domain.meal.MealPlan;
import lombok.*;

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

    public static MealPlanOverviewResponse of(MealPlan plan, List<MealPlanDaySummaryResponse> daySummaries) {
        return MealPlanOverviewResponse.builder()
                .mealPlanId(plan.getId())
                .userId(plan.getUserId())
                .startDate(plan.getStartDate())
                .endDate(plan.getEndDate())
                .targetCaloriesPerDay(plan.getTargetCaloriesPerDay() == null ? 0 : plan.getTargetCaloriesPerDay())
                .days(daySummaries)

                // ✅ A1 필드 포함
                .monthlyBudget(plan.getMonthlyBudget())
                .mealsPerDay(plan.getMealsPerDay())
                .preferences(parseCsv(plan.getPreferences()))
                .allergies(parseCsv(plan.getAllergies()))

                .build();
    }

    // "a,b,c" -> List<String>
    private static List<String> parseCsv(String v) {
        if (v == null || v.isBlank()) return Collections.emptyList();
        return Stream.of(v.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
