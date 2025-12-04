package com.dietcoach.project.controller;

import com.dietcoach.project.common.ApiResponse;
import com.dietcoach.project.dto.meal.MealPlanDetailResponse;
import com.dietcoach.project.dto.meal.MealPlanGenerateRequest;
import com.dietcoach.project.dto.meal.MealPlanSummaryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 식단(MealPlan) 관련 API
 * TODO: MealPlanService 구현 후 의존성 주입 및 실제 로직 연결
 */
@RestController
@RequestMapping("/api")
public class MealPlanController {

    // private final MealPlanService mealPlanService;
    //
    // public MealPlanController(MealPlanService mealPlanService) {
    //     this.mealPlanService = mealPlanService;
    // }

    /**
     * 월간(또는 N일) 식단 생성
     * POST /api/meal-plans
     */
    @PostMapping("/meal-plans")
    public ResponseEntity<ApiResponse<Long>> createMealPlan(@RequestBody MealPlanGenerateRequest request) {
        // TODO: MealPlanService.createMealPlan(request) 호출로 변경
        return ResponseEntity.ok(ApiResponse.error("MealPlan 생성 로직은 아직 구현되지 않았습니다."));
    }

    /**
     * 특정 식단 계획 상세 조회
     * GET /api/meal-plans/{mealPlanId}
     */
    @GetMapping("/meal-plans/{mealPlanId}")
    public ResponseEntity<ApiResponse<MealPlanDetailResponse>> getMealPlan(@PathVariable Long mealPlanId) {
        // TODO: MealPlanService.getMealPlan(mealPlanId) 호출로 변경
        return ResponseEntity.ok(ApiResponse.error("MealPlan 조회 로직은 아직 구현되지 않았습니다."));
    }

    /**
     * 특정 사용자의 최신 식단 요약 조회
     * GET /api/users/{userId}/meal-plans/latest
     */
    @GetMapping("/users/{userId}/meal-plans/latest")  // ← 이렇게 쓰는 게 정석
    public ResponseEntity<ApiResponse<MealPlanSummaryResponse>> getLatestMealPlan(@PathVariable Long userId) {
        // TODO: MealPlanService.getLatestMealPlan(userId) 호출로 변경
        return ResponseEntity.ok(ApiResponse.error("최신 MealPlan 조회 로직은 아직 구현되지 않았습니다."));
    }
}
