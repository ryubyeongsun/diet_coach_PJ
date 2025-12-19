package com.dietcoach.project.service;

import com.dietcoach.project.dto.meal.DashboardSummaryResponse;
import com.dietcoach.project.dto.meal.MealPlanCreateRequest;
import com.dietcoach.project.dto.meal.MealPlanDayDetailResponse;
import com.dietcoach.project.dto.meal.MealPlanIngredientResponse;
import com.dietcoach.project.dto.meal.MealPlanOverviewResponse;

import java.util.List;

public interface MealPlanService {

    // ✅ A1 입력 확장 버전(이것만 사용)
    MealPlanOverviewResponse createMonthlyPlan(Long userId, MealPlanCreateRequest request);

    MealPlanOverviewResponse getMealPlan(Long planId);

    MealPlanOverviewResponse getLatestMealPlanForUser(Long userId);

    List<MealPlanIngredientResponse> getIngredientsForPlan(Long planId);

    DashboardSummaryResponse getDashboardSummary(Long userId);

    MealPlanDayDetailResponse getDayDetail(Long dayId);
}
