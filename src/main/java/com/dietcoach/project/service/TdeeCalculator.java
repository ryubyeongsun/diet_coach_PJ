package com.dietcoach.project.service;

import com.dietcoach.project.domain.ActivityLevel;
import com.dietcoach.project.domain.Gender;
import com.dietcoach.project.domain.GoalType;

/**
 * Provides BMR/TDEE and target calorie calculation.
 */
public final class TdeeCalculator {

    private TdeeCalculator() {
    }

    /**
     * Calculates BMR using Mifflin-St Jeor equation.
     */
    public static double calculateBmr(Gender gender, double weightKg, double heightCm, int age) {
        double base = 10 * weightKg + 6.25 * heightCm - 5 * age;
        if (gender == Gender.FEMALE) {
            return base - 161;
        }
        // Treat MALE/OTHER as male equation for now.
        return base + 5;
    }

    /**
     * Returns TDEE by applying activity multiplier to BMR.
     */
    public static double calculateTdee(double bmr, ActivityLevel level) {
        double factor;
        switch (level) {
            case SEDENTARY -> factor = 1.2;
            case LIGHT -> factor = 1.375;
            case MODERATE -> factor = 1.55;
            case ACTIVE -> factor = 1.725;
            case VERY_ACTIVE -> factor = 1.9;
            default -> factor = 1.2;
        }
        return bmr * factor;
    }

    /**
     * Returns target calorie based on goal type.
     * LOSE: 20% deficit, GAIN: 10% surplus, MAINTAIN: same as TDEE.
     */
    public static double calculateTargetCalorie(double tdee, GoalType goalType) {
        return switch (goalType) {
            case LOSE -> tdee * 0.8;
            case GAIN -> tdee * 1.1;
            case MAINTAIN -> tdee;
        };
    }
}
