package com.dietcoach.project.dto.meal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MealPlanDetailResponse {

    private Long mealPlanId;
    private Long userId;

    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalDays;
    private Double targetCaloriesPerDay;

    private List<MealPlanDayDto> days;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MealPlanDayDto {
        private Long mealPlanDayId;
        private LocalDate planDate;
        private Integer dayIndex;
        private Double totalCalories;
        private List<MealItemDto> items;
        
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MealItemDto {
        private Long mealItemId;
        private String mealTime;
        private String foodName;
        private Double calories;
        private String memo;
    }
}
