package com.dietcoach.project.dto.meal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealIntakeRequest {
    private Long userId; // Optional in body if using @AuthenticationPrincipal, but keeping for completeness if needed
    private Long mealPlanDayId;
    private String mealTime;
    private Boolean isConsumed;
}
