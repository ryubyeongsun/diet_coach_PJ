package com.dietcoach.project.domain.meal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealIntake {
    private Long id;
    private Long userId;
    private Long mealPlanDayId;
    private String mealTime;
    private boolean isConsumed;
    private LocalDateTime consumedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
