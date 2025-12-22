package com.dietcoach.project.service;

import com.dietcoach.project.dto.meal.*;

import java.util.List;

public interface MealPlanService {
    MealPlanOverviewResponse createMonthlyPlan(Long userId, MealPlanCreateRequest request);
    MealPlanOverviewResponse getMealPlan(Long planId);
    MealPlanOverviewResponse getLatestMealPlanForUser(Long userId);
    List<MealPlanIngredientResponse> getIngredientsForPlan(Long planId);
    DashboardSummaryResponse getDashboardSummary(Long userId);
    MealPlanDayDetailResponse getDayDetail(Long dayId);
    ShoppingListResponse getShoppingList(Long planId, String range);

    // A4: 하루 재생성
    MealPlanDayDetailResponse regenerateDay(Long dayId);
    // A4: 끼니 교체
    MealPlanDayDetailResponse replaceMeal(Long dayId, String mealTime);
}