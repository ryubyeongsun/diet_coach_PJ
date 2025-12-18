package com.dietcoach.project.controller;

import com.dietcoach.project.common.ApiResponse;
import com.dietcoach.project.dto.meal.DashboardSummaryResponse;
import com.dietcoach.project.dto.meal.MealPlanCreateRequest;
import com.dietcoach.project.dto.meal.MealPlanDayDetailResponse;
import com.dietcoach.project.dto.meal.MealPlanIngredientResponse;
import com.dietcoach.project.dto.meal.MealPlanOverviewResponse;
import com.dietcoach.project.service.MealPlanService;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MealPlanController {

    private final MealPlanService mealPlanService;

    /**
     * 한 달 식단 자동 생성
     * POST /api/meal-plans
     *
     * ✅ userId는 토큰에서 꺼낸다(@AuthenticationPrincipal Long)
     * ✅ request는 예산/끼니수/선호/알레르기 포함 가능(A1)
     */
    @PostMapping("/meal-plans")
    public ApiResponse<MealPlanOverviewResponse> createMealPlan(
            @AuthenticationPrincipal Long userId,
            @RequestBody MealPlanCreateRequest request
    ) {
        // userId를 바디에서 받지 않고 principal로 강제
        MealPlanOverviewResponse response = mealPlanService.createMonthlyPlan(userId, request);
        return ApiResponse.success("meal plan created", response);
    }

    /**
     * 식단 플랜 상세 조회
     * GET /api/meal-plans/{planId}
     */
    @GetMapping("/meal-plans/{planId}")
    public ApiResponse<MealPlanOverviewResponse> getMealPlan(@PathVariable Long planId) {
        MealPlanOverviewResponse response = mealPlanService.getMealPlan(planId);
        return ApiResponse.success(response);
    }

    /**
     * 유저의 가장 최신 식단 플랜 조회
     * GET /api/users/{userId}/meal-plans/latest
     *
     * (선택) 보안상 principal 기반으로도 하나 더 제공 가능.
     * 지금은 기존 경로 유지.
     */
    @GetMapping("/users/{userId}/meal-plans/latest")
    public ApiResponse<MealPlanOverviewResponse> getLatestMealPlan(@PathVariable Long userId) {
        MealPlanOverviewResponse response = mealPlanService.getLatestMealPlanForUser(userId);
        return ApiResponse.success(response);
    }

    /**
     * (권장) 내 최신 플랜
     * GET /api/meal-plans/latest
     */
    @GetMapping("/meal-plans/latest")
    public ApiResponse<MealPlanOverviewResponse> getMyLatestMealPlan(@AuthenticationPrincipal Long userId) {
        MealPlanOverviewResponse response = mealPlanService.getLatestMealPlanForUser(userId);
        return ApiResponse.success(response);
    }

    /**
     * 플랜 재료 집계
     * GET /api/meal-plans/{planId}/ingredients
     */
    @GetMapping("/meal-plans/{planId}/ingredients")
    public ApiResponse<List<MealPlanIngredientResponse>> getIngredients(@PathVariable Long planId) {
        List<MealPlanIngredientResponse> ingredients = mealPlanService.getIngredientsForPlan(planId);
        return ApiResponse.success(ingredients);
    }

    /**
     * 대시보드 요약
     * GET /api/dashboard/summary
     */
    @GetMapping("/dashboard/summary")
    public ApiResponse<DashboardSummaryResponse> summary(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(mealPlanService.getDashboardSummary(userId));
    }

    /**
     * 하루 상세
     * GET /api/meal-plans/days/{dayId}
     */
    @GetMapping("/meal-plans/days/{dayId}")
    public ApiResponse<MealPlanDayDetailResponse> getDayDetail(@PathVariable Long dayId) {
        MealPlanDayDetailResponse detail = mealPlanService.getDayDetail(dayId);
        return ApiResponse.success(detail);
    }
}
