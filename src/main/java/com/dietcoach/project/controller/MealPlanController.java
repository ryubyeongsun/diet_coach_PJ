package com.dietcoach.project.controller;

import com.dietcoach.project.common.ApiResponse;
import com.dietcoach.project.dto.meal.DashboardSummaryResponse;
import com.dietcoach.project.dto.meal.MealPlanCreateRequest;
import com.dietcoach.project.dto.meal.MealPlanDayDetailResponse;
import com.dietcoach.project.dto.meal.MealPlanIngredientResponse;
import com.dietcoach.project.dto.meal.MealPlanOverviewResponse;
import com.dietcoach.project.dto.meal.ShoppingListResponse;
import com.dietcoach.project.service.MealPlanService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class MealPlanController {

    private final MealPlanService mealPlanService;

    /**
     * 한 달 식단 자동 생성
     * POST /api/meal-plans
     *
     * ✅ userId는 토큰(@AuthenticationPrincipal)에서만 받음
     * ✅ request는 A1: monthlyBudget/mealsPerDay/preferences/allergies 포함 가능
     */
    @PostMapping("/meal-plans")
    public ApiResponse<MealPlanOverviewResponse> createMealPlan(
            @AuthenticationPrincipal Long userId,
            @RequestBody MealPlanCreateRequest request
    ) {
        log.info("[MealPlan] POST /api/meal-plans serviceClass={}", mealPlanService.getClass().getName());
        MealPlanOverviewResponse response = mealPlanService.createMonthlyPlan(userId, request);
        return ApiResponse.success("meal plan created", response);
    }

    /**
     * 식단 플랜 상세 조회
     * GET /api/meal-plans/{planId}
     */
    @GetMapping("/meal-plans/{planId}")
    public ApiResponse<MealPlanOverviewResponse> getMealPlan(@PathVariable Long planId) {
        return ApiResponse.success(mealPlanService.getMealPlan(planId));
    }

    /**
     * 유저의 가장 최신 식단 플랜 조회 (기존 유지)
     * GET /api/users/{userId}/meal-plans/latest
     */
    @GetMapping("/users/{userId}/meal-plans/latest")
    public ApiResponse<MealPlanOverviewResponse> getLatestMealPlan(@PathVariable Long userId) {
        return ApiResponse.success(mealPlanService.getLatestMealPlanForUser(userId));
    }

    /**
     * (권장) 내 최신 플랜
     * GET /api/meal-plans/latest
     */
    @GetMapping("/meal-plans/latest")
    public ApiResponse<MealPlanOverviewResponse> getMyLatestMealPlan(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(mealPlanService.getLatestMealPlanForUser(userId));
    }

    /**
     * 플랜 재료 집계
     * GET /api/meal-plans/{planId}/ingredients
     */
    @GetMapping("/meal-plans/{planId}/ingredients")
    public ApiResponse<List<MealPlanIngredientResponse>> getIngredients(@PathVariable Long planId) {
        return ApiResponse.success(mealPlanService.getIngredientsForPlan(planId));
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
        return ApiResponse.success(mealPlanService.getDayDetail(dayId));
    }
    @GetMapping("/meal-plans/{planId}/shopping")
    public ApiResponse<ShoppingListResponse> getShopping(
            @PathVariable Long planId,
            @RequestParam(defaultValue = "MONTH") String range
    ) {
        return ApiResponse.success(mealPlanService.getShoppingList(planId, range));
    }
}
