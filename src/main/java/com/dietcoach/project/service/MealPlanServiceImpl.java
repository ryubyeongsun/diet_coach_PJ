package com.dietcoach.project.service;

import com.dietcoach.project.dto.meal.MealPlanDaySummaryResponse;
import com.dietcoach.project.dto.meal.MealPlanOverviewResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;

@Service
public class MealPlanServiceImpl implements MealPlanService {

    @Override
    public MealPlanOverviewResponse getOverviewForUser(Long userId) {
        // TODO: 나중에 userId 기반으로 실제 식단 계획/칼로리/진행률 계산 로직 붙이기

        return MealPlanOverviewResponse.builder()
                .weeklyAvgIntake(1820)
                .weeklyAvgSpend(2050)
                .achievementRate(0.80)   // 80%
                .weightChange(-0.6)      // -0.6kg
                .days(Arrays.asList(
                        MealPlanDaySummaryResponse.builder()
                                .date(LocalDate.of(2025, 12, 1))
                                .totalCalories(1800)
                                .label("아·점·저 균형식")
                                .build(),
                        MealPlanDaySummaryResponse.builder()
                                .date(LocalDate.of(2025, 12, 2))
                                .totalCalories(1750)
                                .label("탄수 조금 낮춤")
                                .build(),
                        MealPlanDaySummaryResponse.builder()
                                .date(LocalDate.of(2025, 12, 3))
                                .totalCalories(1900)
                                .label("운동 후 고단백")
                                .build(),
                        MealPlanDaySummaryResponse.builder()
                                .date(LocalDate.of(2025, 12, 4))
                                .totalCalories(1700)
                                .label("저녁 야식 컷")
                                .build()
                ))
                .build();
    }
}
