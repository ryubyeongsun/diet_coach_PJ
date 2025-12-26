package com.dietcoach.project.dto.meal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MealPlanCreateRequest {

    private Long userId;
    private LocalDate startDate;

    @Min(0)
    private Long monthlyBudget;

    @Min(1)
    @Max(3)
    private Integer mealsPerDay;

    private List<String> preferences;
    private List<String> allergies;
}
