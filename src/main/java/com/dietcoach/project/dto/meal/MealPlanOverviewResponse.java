package com.dietcoach.project.dto.meal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 대시보드 상단에 보여줄 "이번 달 식단 요약" 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MealPlanOverviewResponse {

    // 상단 큰 카드에서 쓸 값들
    private int weeklyAvgIntake;      // 이번 주 평균 섭취 kcal
    private int weeklyAvgSpend;       // 이번 주 평균 소비 kcal
    private double achievementRate;   // 이번 달 목표 달성률 (0.0 ~ 1.0)
    private double weightChange;      // 이번 주 체중 변화 (kg)

    // 하단 "이번 달 식단 개요" 카드 리스트
    private List<MealPlanDaySummaryResponse> days;
}
