package com.dietcoach.project.dto.meal;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 대시보드 상단 카드용 요약 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummaryResponse {

    private Long userId;
    private Long recentMealPlanId;

    private LocalDate startDate;
    private LocalDate endDate;
    private int totalDays;

    private int targetCaloriesPerDay;
    private int averageCalories;
    private int achievementRate; // %
}
