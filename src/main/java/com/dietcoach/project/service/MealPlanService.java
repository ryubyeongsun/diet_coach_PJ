package com.dietcoach.project.service;

import com.dietcoach.project.dto.meal.MealPlanOverviewResponse;

import java.time.LocalDate;

public interface MealPlanService {

    MealPlanOverviewResponse createMonthlyPlan(Long userId, LocalDate startDate);

    MealPlanOverviewResponse getMealPlan(Long planId);

    MealPlanOverviewResponse getLatestMealPlanForUser(Long userId);
}
