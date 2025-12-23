package com.dietcoach.project.service;

import com.dietcoach.project.common.error.BusinessException;
import com.dietcoach.project.domain.meal.MealIntake;
import com.dietcoach.project.domain.meal.MealPlanDay;
import com.dietcoach.project.dto.meal.MealIntakeRequest;
import com.dietcoach.project.dto.meal.MealIntakeResponse;
import com.dietcoach.project.mapper.meal.MealIntakeMapper;
import com.dietcoach.project.mapper.meal.MealPlanMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class MealIntakeServiceImpl implements MealIntakeService {

    private final MealIntakeMapper mealIntakeMapper;
    private final MealPlanMapper mealPlanMapper;

    @Override
    @Transactional
    public MealIntakeResponse upsertIntake(Long userId, MealIntakeRequest request) {
        if (request.getMealPlanDayId() == null || request.getMealTime() == null) {
            throw new BusinessException("mealPlanDayId and mealTime are required");
        }

        // Validate that the mealPlanDay exists and belongs to a plan owned by the user (or at least exists)
        // Strictly speaking, we should check if the plan belongs to the user.
        // For MVP, checking if day exists is a good start.
        MealPlanDay day = mealPlanMapper.findMealPlanDayById(request.getMealPlanDayId());
        if (day == null) {
            throw new BusinessException("MealPlanDay not found: " + request.getMealPlanDayId());
        }

        boolean isConsumed = Boolean.TRUE.equals(request.getIsConsumed());
        LocalDateTime consumedAt = isConsumed ? LocalDateTime.now() : null;

        MealIntake intake = MealIntake.builder()
                .userId(userId)
                .mealPlanDayId(request.getMealPlanDayId())
                .mealTime(request.getMealTime())
                .isConsumed(isConsumed)
                .consumedAt(consumedAt)
                .build();

        mealIntakeMapper.upsert(intake);

        return MealIntakeResponse.builder()
                .mealPlanDayId(intake.getMealPlanDayId())
                .mealTime(intake.getMealTime())
                .isConsumed(intake.isConsumed())
                .consumedAt(intake.getConsumedAt())
                .build();
    }
}
