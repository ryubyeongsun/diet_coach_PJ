package com.dietcoach.project.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 날짜별 트렌드
 * - date: yyyy-MM-dd
 * - totalCalories: 해당 날짜 섭취 칼로리(없으면 null)
 * - targetCalories: 유저 목표 칼로리(가능하면 users.target_calories, 없으면 meal_plans 값)
 * - weight: 해당 날짜 체중(없으면 null)
 * - achievementRate: totalCalories/targetCalories*100 (계산 불가면 null)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DayTrend {
    private String date;
    private Integer totalCalories;
    private Integer targetCalories;
    private Double weight;
    private Integer achievementRate;
}
