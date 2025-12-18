package com.dietcoach.project.service;

import com.dietcoach.project.dto.meal.DashboardSummaryResponse;
import com.dietcoach.project.dto.meal.MealPlanCreateRequest;
import com.dietcoach.project.dto.meal.MealPlanDayDetailResponse;
import com.dietcoach.project.dto.meal.MealPlanIngredientResponse;
import com.dietcoach.project.dto.meal.MealPlanOverviewResponse;

import java.time.LocalDate;
import java.util.List;

public interface MealPlanService {

    MealPlanOverviewResponse createMonthlyPlan(Long userId, LocalDate startDate);

    MealPlanOverviewResponse getMealPlan(Long planId);

    MealPlanOverviewResponse getLatestMealPlanForUser(Long userId);
    List<MealPlanIngredientResponse> getIngredientsForPlan(Long planId);
    DashboardSummaryResponse getDashboardSummary(Long userId);
    MealPlanDayDetailResponse getDayDetail(Long dayId);
    MealPlanOverviewResponse createMonthlyPlan(Long userId, MealPlanCreateRequest request);
}
