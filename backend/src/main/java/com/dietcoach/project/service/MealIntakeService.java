package com.dietcoach.project.service;

import com.dietcoach.project.dto.meal.MealIntakeRequest;
import com.dietcoach.project.dto.meal.MealIntakeResponse;

public interface MealIntakeService {
    MealIntakeResponse upsertIntake(Long userId, MealIntakeRequest request);
}
