package com.dietcoach.project.service;

import com.dietcoach.project.dto.meal.MealPlanOverviewResponse;

public interface MealPlanService {

    /**
     * 아직은 DB / 알고리즘 없이, 데모용 더미 데이터 반환
     */
    MealPlanOverviewResponse getOverviewForUser(Long userId);
}
