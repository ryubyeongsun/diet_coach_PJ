package com.dietcoach.project.controller;

import com.dietcoach.project.common.ApiResponse;
import com.dietcoach.project.dto.meal.MealPlanCreateRequest;
import com.dietcoach.project.dto.meal.MealPlanOverviewResponse;
import com.dietcoach.project.service.MealPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MealPlanController {

    private final MealPlanService mealPlanService;

    /**
     * 한 달 식단 자동 생성
     * POST /api/meal-plans
     */
    @PostMapping("/meal-plans")
    public ApiResponse<MealPlanOverviewResponse> createMealPlan(
            @RequestBody MealPlanCreateRequest request
    ) {
        MealPlanOverviewResponse response =
                mealPlanService.createMonthlyPlan(request.getUserId(), request.getStartDate());
        return ApiResponse.success("meal plan created", response);
    }

    /**
     * 식단 플랜 상세 조회
     * GET /api/meal-plans/{planId}
     */
    @GetMapping("/meal-plans/{planId}")
    public ApiResponse<MealPlanOverviewResponse> getMealPlan(
            @PathVariable Long planId
    ) {
        MealPlanOverviewResponse response = mealPlanService.getMealPlan(planId);
        return ApiResponse.success(response);
    }

    /**
     * 유저의 가장 최신 식단 플랜 조회
     * GET /api/users/{userId}/meal-plans/latest
     */
    @GetMapping("/users/{userId}/meal-plans/latest")
    public ApiResponse<MealPlanOverviewResponse> getLatestMealPlan(
            @PathVariable Long userId
    ) {
        MealPlanOverviewResponse response = mealPlanService.getLatestMealPlanForUser(userId);
        if (response == null) {
            return ApiResponse.error("해당 유저의 식단 플랜이 없습니다.");
        }
        return ApiResponse.success(response);
    }
}
