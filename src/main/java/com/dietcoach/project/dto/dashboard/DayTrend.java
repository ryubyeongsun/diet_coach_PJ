package com.dietcoach.project.dto.dashboard;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DayTrend {
    private String date;            // "YYYY-MM-DD"
    private Integer totalCalories;  // meal_plan_days.total_calories (없으면 null)
    private Integer targetCalories; // users.target_calories
    private Double weight;          // weight_records.weight (없으면 null)
    private Integer achievementRate;
}
