package com.dietcoach.project.domain.meal;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealPlanDay {

    private Long id;
    private Long mealPlanId;

    private LocalDate planDate;
    private Integer dayIndex;
    private Integer totalCalories;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
