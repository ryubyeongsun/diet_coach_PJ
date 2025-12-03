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
public class MealPlan {

    private Long id;
    private Long userId;

    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalDays;
    private Double targetCaloriesPerDay;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
