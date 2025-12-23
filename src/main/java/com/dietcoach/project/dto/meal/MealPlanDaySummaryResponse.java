package com.dietcoach.project.dto.meal;

import java.time.LocalDate;
import java.util.List;

import com.dietcoach.project.domain.meal.MealItem;
import com.dietcoach.project.domain.meal.MealPlanDay;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 한 달 식단 중 "하루" 요약 정보 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MealPlanDaySummaryResponse {

    private Long dayId;          // meal_plan_days.id
    private LocalDate date;      // 2025-12-01
    private int totalCalories;   // 1800
    private String label;        // "아·점·저 균형식" 같은 요약 문구
    private String memo;         // "AI" | "TEMPLATE"
    private String representativeMenu; // 대표 메뉴 (칼로리 가장 높은 음식)

    public static MealPlanDaySummaryResponse from(
            MealPlanDay day,
            List<MealItem> items
    ) {
        int sumCalories = items.stream()
                .mapToInt(MealItem::getCalories)
                .sum();
        int totalCalories = (day.getTotalCalories() != null && day.getTotalCalories() > 0)
                ? day.getTotalCalories()
                : sumCalories;

        // 대표 메뉴 추출 (칼로리 내림차순 1순위)
        String representativeMenu = items.stream()
                .sorted((a, b) -> b.getCalories() - a.getCalories())
                .findFirst()
                .map(MealItem::getFoodName)
                .orElse(null);

        // 일단 1차 버전은 고정 라벨(나중에 규칙 넣어도 됨)
        String label = "자동 생성 균형 식단";
        boolean hasAi = items.stream()
                .map(MealItem::getMemo)
                .anyMatch(m -> m != null && m.startsWith("AI|"));
        String memo = hasAi ? "AI" : "TEMPLATE";

        return MealPlanDaySummaryResponse.builder()
                .dayId(day.getId())
                .date(day.getPlanDate())
                .totalCalories(totalCalories)
                .label(label)
                .memo(memo)
                .representativeMenu(representativeMenu)
                .build();
    }
}
