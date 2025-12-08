package com.dietcoach.project.domain.meal;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealPlan {

    private Long id;
    private Long userId;

    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalDays;
    private Integer targetCaloriesPerDay;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
