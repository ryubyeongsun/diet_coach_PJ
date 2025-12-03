package com.dietcoach.project.service;

import com.dietcoach.project.dto.meal.MealPlanDetailResponse;
import com.dietcoach.project.dto.meal.MealPlanGenerateRequest;
import com.dietcoach.project.dto.meal.MealPlanSummaryResponse;

public interface MealPlanService {

    Long createMealPlan(MealPlanGenerateRequest request);

    MealPlanDetailResponse getMealPlan(Long mealPlanId);

    MealPlanSummaryResponse getLatestMealPlan(Long userId);
}
