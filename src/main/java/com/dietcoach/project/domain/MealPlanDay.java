package com.dietcoach.project.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MealPlanDay {

    private Long id;
    private Long mealPlanId;

    private LocalDate planDate;
    private Double totalCalories;
    private Integer dayIndex;  

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
