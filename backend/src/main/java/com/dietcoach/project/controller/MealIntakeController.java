package com.dietcoach.project.controller;

import com.dietcoach.project.common.ApiResponse;
import com.dietcoach.project.dto.meal.MealIntakeRequest;
import com.dietcoach.project.dto.meal.MealIntakeResponse;
import com.dietcoach.project.service.MealIntakeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/meal-intakes")
@RequiredArgsConstructor
@Slf4j
public class MealIntakeController {

    private final MealIntakeService mealIntakeService;

    @PutMapping
    public ApiResponse<MealIntakeResponse> upsertIntake(
            @AuthenticationPrincipal Long userId,
            @RequestBody MealIntakeRequest request
    ) {
        return ApiResponse.success(mealIntakeService.upsertIntake(userId, request));
    }
}
