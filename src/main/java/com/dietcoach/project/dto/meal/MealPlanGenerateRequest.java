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
public class MealPlanGenerateRequest {

    private Long userId;                // 어떤 유저의 식단인지
    private LocalDate startDate;        // 식단 시작일
    private Integer totalDays;          // 몇 일짜리 식단인지 (예: 30)
    private Double targetCaloriesPerDay; // 1일 목표 칼로리 (null이면 user.targetCalories 사용)
}
