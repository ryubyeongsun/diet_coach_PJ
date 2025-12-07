package com.dietcoach.project.controller;

import com.dietcoach.project.common.ApiResponse;
import com.dietcoach.project.dto.meal.MealPlanOverviewResponse;
import com.dietcoach.project.service.MealPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 한 달 식단 플랜 관련 API
 */
@RestController
@RequestMapping("/api/meal-plans")
@RequiredArgsConstructor
public class MealPlanController {

    private final MealPlanService mealPlanService;

    /**
     * 대시보드 상단에 보여줄 "이번 달 식단 개요" 조회
     * 예: GET /api/meal-plans/overview?userId=1
     */
    @GetMapping("/overview")
    public ApiResponse<MealPlanOverviewResponse> getOverview(
            @RequestParam Long userId
    ) {
        MealPlanOverviewResponse overview = mealPlanService.getOverviewForUser(userId);
        return ApiResponse.success("meal plan overview loaded", overview);
    }
}
