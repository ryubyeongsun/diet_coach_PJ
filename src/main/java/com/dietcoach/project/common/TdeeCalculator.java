package com.dietcoach.project.common;

import java.time.LocalDate;
import java.time.Period;

import com.dietcoach.project.domain.ActivityLevel;
import com.dietcoach.project.domain.Gender;
import com.dietcoach.project.domain.GoalType;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * BMR / TDEE / 목표 칼로리 계산 유틸
 * - 어디서든 재사용 가능하도록 static 메서드로만 구성
 */
public class TdeeCalculator {

    @Getter
    @AllArgsConstructor
    public static class TdeeResult {
        private double bmr;
        private double tdee;
        private double targetCalories;
    }

    public static TdeeResult calculate(
            Gender gender,
            LocalDate birthDate,
            double heightCm,
            double weightKg,
            ActivityLevel activityLevel,
            GoalType goalType
    ) {
        int age = calculateAge(birthDate);
        double bmr = calculateBmr(gender, weightKg, heightCm, age);
        double tdee = calculateTdee(bmr, activityLevel);
        double targetCalories = applyGoal(tdee, goalType);
        return new TdeeResult(bmr, tdee, targetCalories);
    }

    private static int calculateAge(LocalDate birthDate) {
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    // Mifflin-St Jeor 공식
    private static double calculateBmr(Gender gender, double weightKg, double heightCm, int age) {
        double base = 10 * weightKg + 6.25 * heightCm - 5 * age;
        if (gender == Gender.MALE) {
            return base + 5;
        } else {
            return base - 161;
        }
    }

    // 활동 레벨 계수 적용
    private static double calculateTdee(double bmr, ActivityLevel level) {
        double factor;
        switch (level) {
            case SEDENTARY:   factor = 1.2;   break;
            case LIGHT:       factor = 1.375; break;
            case MODERATE:    factor = 1.55;  break;
            case ACTIVE:      factor = 1.725; break;
            case VERY_ACTIVE: factor = 1.9;   break;
            default:          factor = 1.2;
        }
        return bmr * factor;
    }

    // 목표 타입에 따른 칼로리 증감
    private static double applyGoal(double tdee, GoalType goalType) {
        switch (goalType) {
            case LOSE_WEIGHT:
                return tdee - 500;    // 감량
            case GAIN_WEIGHT:
                return tdee + 300;    // 증량
            case MAINTAIN:
            default:
                return tdee;          // 유지
        }
    }
}
