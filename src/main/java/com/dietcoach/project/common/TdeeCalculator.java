package com.dietcoach.project.common;

import java.time.LocalDate;
import java.time.Period;

import com.dietcoach.project.domain.ActivityLevel;
import com.dietcoach.project.domain.Gender;
import com.dietcoach.project.domain.GoalType;

import lombok.Builder;
import lombok.Getter;

public class TdeeCalculator {

    @Getter
    @Builder
    public static class TdeeResult {
        private Double bmr;
        private Double tdee;
        private Double targetCalories;
    }

    public static TdeeResult calculate(
            Gender gender,
            LocalDate birthDate,
            Double height,         // cm
            Double weight,         // kg
            ActivityLevel activityLevel,
            GoalType goalType
    ) {
        // 1) 나이 계산
        int age = 30;
        if (birthDate != null) {
            age = Period.between(birthDate, LocalDate.now()).getYears();
        }

        // 2) BMR 계산 (Mifflin-St Jeor)
        double bmr;
        if (gender == Gender.MALE) {
            bmr = 10 * weight + 6.25 * height - 5 * age + 5;
        } else {
            bmr = 10 * weight + 6.25 * height - 5 * age - 161;
        }

        // 3) 활동계수 적용해서 TDEE 계산
        double factor;
        switch (activityLevel) {
            case SEDENTARY -> factor = 1.2;
            case LIGHTLY_ACTIVE -> factor = 1.375;
            case MODERATE -> factor = 1.55;
            case VERY_ACTIVE -> factor = 1.725;
            case SUPER_ACTIVE -> factor = 1.9;
            default -> factor = 1.2;
        }

        double tdee = bmr * factor;

        // 4) 목표 칼로리 계산
        double targetCalories = switch (goalType) {
            case LOSE_WEIGHT -> tdee - 500;  // 감량
            case MAINTAIN   -> tdee;         // 유지
            case GAIN_WEIGHT -> tdee + 300;  // 증량
        };

        return TdeeResult.builder()
                .bmr(bmr)
                .tdee(tdee)
                .targetCalories(targetCalories)
                .build();
    }

    // (선택) User 엔티티에 바로 채워넣는 헬퍼
    // 지금 구조랑 충돌 안 나게, 기존 calculate()를 그대로 재사용
    public static void fillUserEnergyFields(com.dietcoach.project.domain.User user) {
        TdeeResult result = calculate(
                user.getGender(),
                user.getBirthDate(),
                user.getHeight(),
                user.getWeight(),
                user.getActivityLevel(),
                user.getGoalType()
        );

        user.setBmr(result.getBmr());
        user.setTdee(result.getTdee());
        user.setTargetCalories(result.getTargetCalories());
    }
}
